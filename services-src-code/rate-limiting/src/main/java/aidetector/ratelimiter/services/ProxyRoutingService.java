package aidetector.ratelimiter.services;

import aidetector.ratelimiter.config.GatewayRoutingProperties;
import aidetector.ratelimiter.filters.ProtectedTokenRateLimitFilter;
import aidetector.ratelimiter.filters.TokenCreationRateLimitFilter;
import aidetector.ratelimiter.utils.LogContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProxyRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(ProxyRoutingService.class);

    private final RestClient restClient;
    private final String tokenRouteId;
    private final String forwardedClientIpHeaderName;
    private final String gatewayAuthHeaderName;
    private final String gatewayAuthSharedSecret;
    private final Set<String> trustedForwardedHeaders;
    private final String isAnonymHeader;

    public ProxyRoutingService(
            RestClient.Builder restClientBuilder,
            @Value("${api-gateway-service.routing.token-route-id:token-manager-token-route}")
            String tokenRouteId,
            @Value("${api-gateway-service.network.forwarded-client-ip-header:X-Gateway-Client-Ip}")
            String forwardedClientIpHeaderName,
            @Value("${api-gateway-service.security.authentication.gateway-shared-secret-header:X-Gateway-Auth}")
            String gatewayAuthHeaderName,
            @Value("${api-gateway-service.security.authentication.gateway-shared-secret}")
            String gatewayAuthSharedSecret,
            @Value("#{'${api-gateway-service.network.trusted-forwarded-headers:X-Real-IP,X-Forwarded-For,X-Forwarded-Proto,X-Forwarded-Host,X-Forwarded-Port,Forwarded}'.split(',')}")
            List<String> trustedForwardedHeaders,
            @Value("${api-gateway-service.network.is-anonymous-user-header}")
            String isAnonymHeader

    ) {
        this.restClient = restClientBuilder.build();
        this.tokenRouteId = tokenRouteId;
        this.forwardedClientIpHeaderName = forwardedClientIpHeaderName;
        this.gatewayAuthHeaderName = gatewayAuthHeaderName;
        this.gatewayAuthSharedSecret = gatewayAuthSharedSecret;
        this.trustedForwardedHeaders = trustedForwardedHeaders.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        this.isAnonymHeader = isAnonymHeader;
    }

    public void forward(HttpServletRequest request,
                        HttpServletResponse response,
                        GatewayRoutingProperties.DownstreamRoute route) throws IOException {
        URI targetUri = buildTargetUri(request, route.getUri());
        LogContext.setEventContext(LogContext.EVENT_PROXY_FORWARD, null, null);
        LogContext.addDetail(LogContext.TARGET_URI, targetUri);
        LogContext.addDetail(LogContext.ROUTE_ID, route.getId());
        logger.info("forwarding request to downstream service");

        try {
            RestClient.RequestBodySpec requestSpec = restClient.method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUri)
                    .headers(headers -> copyRequestHeaders(request, headers, route, request.getContentLengthLong()));

            if (request.getContentLengthLong() > 0 || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
                long finalContentLength = request.getContentLengthLong();
                InputStreamResource resource = new InputStreamResource(request.getInputStream()) {
                    @Override
                    public long contentLength() {
                        return finalContentLength;
                    }
                };
                requestSpec.body(resource);
            }

            requestSpec.exchange((clientRequest, clientResponse) -> {
                response.setStatus(clientResponse.getStatusCode().value());
                clientResponse.getHeaders().forEach((key, value) -> {
                    if (!HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(key)
                            && !HttpHeaders.CONNECTION.equalsIgnoreCase(key)
                            && !key.startsWith(":")) {
                        for (String v : value) {
                            response.addHeader(key, v);
                        }
                    }
                });

                if (clientResponse.getBody() != null) {
                    StreamUtils.copy(clientResponse.getBody(), response.getOutputStream());
                }
                return null;
            });

            LogContext.addDetail(LogContext.STATUS, response.getStatus());
            logger.info("downstream service responded");
        } finally {
            LogContext.clear();
        }
    }

    private URI buildTargetUri(HttpServletRequest request, String baseUri) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUri)
                .path(request.getRequestURI());

        String rawQuery = request.getQueryString();
        if (rawQuery != null && !rawQuery.isBlank()) {
            builder.query(rawQuery);
        }

        return builder.build(true).toUri();
    }

    private void copyRequestHeaders(HttpServletRequest request,
                                    HttpHeaders headers,
                                    GatewayRoutingProperties.DownstreamRoute route,
                                    long contentLength) {
        boolean isTokenRoute = tokenRouteId.equals(route.getId());
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!shouldForwardClientHeader(headerName, isTokenRoute)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                headers.add(headerName, values.nextElement());
            }
        }

        headers.set(gatewayAuthHeaderName, gatewayAuthSharedSecret);

        if (isTokenRoute
                && request.getAttribute(TokenCreationRateLimitFilter.CLIENT_IP_REQUEST_ATTRIBUTE) instanceof String ip) {
            headers.set(forwardedClientIpHeaderName, ip);
        }
        headers.set(isAnonymHeader,String.valueOf(true)); // ici je doit ajouter le header lui même

        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
    }

    private boolean shouldForwardClientHeader(String headerName, boolean isTokenRoute) {
        if (HttpHeaders.HOST.equalsIgnoreCase(headerName)
                || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
                || HttpHeaders.CONNECTION.equalsIgnoreCase(headerName)
                || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)
                || (isTokenRoute && forwardedClientIpHeaderName.equalsIgnoreCase(headerName))
                || (isTokenRoute && gatewayAuthHeaderName.equalsIgnoreCase(headerName))) {
            return false;
        }

        String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
        if (normalizedHeaderName.startsWith("x-") || "forwarded".equals(normalizedHeaderName)) {
            return trustedForwardedHeaders.contains(normalizedHeaderName);
        }

        return true;
    }
}
