package aidetector.ratelimiter.filters;

import aidetector.ratelimiter.exceptions.InvalidTokenException;
import aidetector.ratelimiter.exceptions.RateLimitReachedException;
import aidetector.ratelimiter.model.RateLimiterResponse;
import aidetector.ratelimiter.model.VerifiedToken;
import aidetector.ratelimiter.services.RateLimitingManager;
import aidetector.ratelimiter.services.ReactiveResponseWriter;
import aidetector.ratelimiter.services.TokenVerificationService;
import aidetector.ratelimiter.utils.LogContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class AnonymousGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AnonymousGatewayFilter.class);

    private static final String ROUTE_METADATA_KEY = "access-policy";
    private static final String TOKEN_ROUTE_POLICY = "public-token";
    private static final String ANONYMOUS_PROTECTED_POLICY = "protected-anonymous";
    private static final String CONNECTED_USER_ROUTE_MESSAGE =
            "Connected user flow is not implemented yet on this gateway";

    private final TokenVerificationService tokenVerificationService;
    private final RateLimitingManager rateLimitingManager;
    private final ReactiveResponseWriter responseWriter;
    private final String realIpHeaderName;
    private final String forwardedClientIpHeaderName;
    private final String gatewayAuthHeaderName;
    private final String gatewayAuthSharedSecret;
    private final Set<String> trustedForwardedHeaders;
    private final String isAnonymHeader;

    public AnonymousGatewayFilter(
            TokenVerificationService tokenVerificationService,
            RateLimitingManager rateLimitingManager,
            ReactiveResponseWriter responseWriter,
            @Value("${api-gateway-service.network.real-ip-header:X-Real-IP}")
            String realIpHeaderName,
            @Value("${api-gateway-service.network.forwarded-client-ip-header:X-Gateway-Client-Ip}")
            String forwardedClientIpHeaderName,
            @Value("${api-gateway-service.security.authentication.gateway-shared-secret-header:X-Gateway-Auth}")
            String gatewayAuthHeaderName,
            @Value("${api-gateway-service.security.authentication.gateway-shared-secret}")
            String gatewayAuthSharedSecret,
            @Value("#{'${api-gateway-service.network.trusted-forwarded-headers:X-Real-IP,X-Forwarded-For,X-Forwarded-Proto,X-Forwarded-Host,X-Forwarded-Port,Forwarded}'.split(',')}")
            List<String> trustedForwardedHeaders,
            @Value("${api-gateway-service.network.is-anonymous-user-header}")
            String isAnonymHeader) {
        this.tokenVerificationService = tokenVerificationService;
        this.rateLimitingManager = rateLimitingManager;
        this.responseWriter = responseWriter;
        this.realIpHeaderName = realIpHeaderName;
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

    @Override
    @NullMarked
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        String accessPolicy = String.valueOf(route.getMetadata().getOrDefault(ROUTE_METADATA_KEY, ""));
        if (TOKEN_ROUTE_POLICY.equals(accessPolicy)) {
            return handleTokenRoute(exchange, chain, route);
        }
        if (ANONYMOUS_PROTECTED_POLICY.equals(accessPolicy)) {
            return handleAnonymousProtectedRoute(exchange, chain, route);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> handleTokenRoute(ServerWebExchange exchange, GatewayFilterChain chain, Route route) {
        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, null, null);
        try {
            String normalizedIp = normalizeIp(exchange.getRequest().getHeaders().getFirst(realIpHeaderName));
            LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, normalizedIp, null);
            LogContext.addDetail(LogContext.STATUS, "SUCCESS");
            LogContext.addDetail(LogContext.ROUTE_ID, route.getId());
            logger.info("ip verification succeeded");

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(mutateRequest(exchange.getRequest(), true, normalizedIp, true))
                    .build();
            return chain.filter(mutatedExchange)
                    .doFinally(signalType -> LogContext.clear());
        } catch (UnknownHostException | IllegalArgumentException exception) {
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            LogContext.addDetail(LogContext.MALFORMED_IP, exchange.getRequest().getHeaders().getFirst(realIpHeaderName));
            LogContext.addDetail(LogContext.EXCEPTION_MSG, exception.getMessage());
            logger.warn("ip format not valid or null");
            return responseWriter.write(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    new RateLimiterResponse(null, "ip format not valid or null")
            ).doFinally(signalType -> LogContext.clear());
        }
    }

    private Mono<Void> handleAnonymousProtectedRoute(ServerWebExchange exchange,
                                                     GatewayFilterChain chain,
                                                     Route route) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK, null, null);
        LogContext.addDetail(LogContext.ROUTE_ID, route.getId());

        if (authHeader == null || authHeader.isBlank()) {
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            logger.warn("No token present");
            return responseWriter.write(
                    exchange,
                    HttpStatus.BAD_REQUEST,
                    new RateLimiterResponse(null, "No token present")
            ).doFinally(signalType -> LogContext.clear());
        }

        if (authHeader.startsWith("Bearer ")) {
            LogContext.addDetail(LogContext.STATUS, "NOT_IMPLEMENTED");
            logger.warn("connected user flow not implemented");
            return responseWriter.write(
                    exchange,
                    HttpStatus.NOT_IMPLEMENTED,
                    new RateLimiterResponse(null, CONNECTED_USER_ROUTE_MESSAGE)
            ).doFinally(signalType -> LogContext.clear());
        }

        if (!authHeader.startsWith("Anonym ")) {
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            logger.warn("authorization schema not implemented");
            return responseWriter.write(
                    exchange,
                    HttpStatus.NOT_IMPLEMENTED,
                    new RateLimiterResponse(null, "Authorization schema not implemented yet")
            ).doFinally(signalType -> LogContext.clear());
        }

        return Mono.fromCallable(() -> verifyAnonymousToken(authHeader.substring(7)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(verifiedToken -> {
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(mutateRequest(exchange.getRequest(), false, null, true))
                            .build();
                    return chain.filter(mutatedExchange)
                            .then(incrementOnSuccess(mutatedExchange, verifiedToken))
                            .doFinally(signalType -> LogContext.clear());
                })
                .onErrorResume(RateLimitReachedException.class, exception -> {
                    LogContext.addDetail(LogContext.STATUS, "FAILED");
                    logger.warn("rate limit reached for token");
                    return responseWriter.write(
                            exchange,
                            HttpStatus.TOO_MANY_REQUESTS,
                            new RateLimiterResponse(true, exception.getMessage())
                    ).doFinally(signalType -> LogContext.clear());
                })
                .onErrorResume(InvalidTokenException.class, exception -> {
                    LogContext.addDetail(LogContext.STATUS, "FAILED");
                    logger.warn("token validation failed");
                    return responseWriter.write(
                            exchange,
                            HttpStatus.BAD_REQUEST,
                            new RateLimiterResponse(null, exception.getMessage())
                    ).doFinally(signalType -> LogContext.clear());
                });
    }

    private VerifiedToken verifyAnonymousToken(String token) throws RateLimitReachedException {
        VerifiedToken verifiedToken = tokenVerificationService.verify(token);
        LogContext.clear();
        LogContext.setEventContext(
                LogContext.EVENT_RATE_LIMIT_CHECK,
                verifiedToken.userIp(),
                verifiedToken.userId()
        );
        rateLimitingManager.verifyAttemptsLimit(verifiedToken.tokenHash());
        return verifiedToken;
    }

    private Mono<Void> incrementOnSuccess(ServerWebExchange exchange, VerifiedToken verifiedToken) {
        HttpStatus status = exchange.getResponse().getStatusCode() instanceof HttpStatus httpStatus
                ? httpStatus
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (!status.is2xxSuccessful()) {
            return Mono.empty();
        }
        return Mono.fromRunnable(() -> rateLimitingManager.incrementKeyValue(verifiedToken.tokenHash()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private ServerHttpRequest mutateRequest(ServerHttpRequest request,
                                            boolean isTokenRoute,
                                            String normalizedIp,
                                            boolean isAnonymousRequest) {
        return request.mutate()
                .headers(headers -> {
                    sanitizeHeaders(headers, isTokenRoute);
                    headers.set(gatewayAuthHeaderName, gatewayAuthSharedSecret);
                    headers.set(isAnonymHeader, String.valueOf(isAnonymousRequest));
                    if (isTokenRoute && normalizedIp != null) {
                        headers.set(forwardedClientIpHeaderName, normalizedIp);
                    }
                })
                .build();
    }

    private void sanitizeHeaders(HttpHeaders headers, boolean isTokenRoute) {
        Set<String> headerNames = headers.headerSet().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
        for (String headerName : headerNames) {
            if (!shouldForwardClientHeader(headerName, isTokenRoute)) {
                headers.remove(headerName);
            }
        }
    }

    private boolean shouldForwardClientHeader(String headerName, boolean isTokenRoute) {
        if (HttpHeaders.HOST.equalsIgnoreCase(headerName)
                || HttpHeaders.CONTENT_LENGTH.equalsIgnoreCase(headerName)
                || HttpHeaders.CONNECTION.equalsIgnoreCase(headerName)
                || HttpHeaders.TRANSFER_ENCODING.equalsIgnoreCase(headerName)
                || forwardedClientIpHeaderName.equalsIgnoreCase(headerName)
                || gatewayAuthHeaderName.equalsIgnoreCase(headerName)
                || isAnonymHeader.equalsIgnoreCase(headerName)) {
            return false;
        }

        String normalizedHeaderName = headerName.toLowerCase(Locale.ROOT);
        if (normalizedHeaderName.startsWith("x-") || "forwarded".equals(normalizedHeaderName)) {
            return trustedForwardedHeaders.contains(normalizedHeaderName);
        }

        return true;
    }

    private String normalizeIp(String rawIp) throws UnknownHostException {
        if (rawIp == null || rawIp.isBlank()) {
            throw new IllegalArgumentException("missing mandatory real ip header");
        }
        return InetAddress.getByName(rawIp.trim()).getHostAddress();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
