package aidetector.ratelimiter.filters;

import aidetector.ratelimiter.model.RateLimiterResponse;
import aidetector.ratelimiter.services.RateLimitResponseWriter;
import aidetector.ratelimiter.services.RouteResolutionService;
import aidetector.ratelimiter.utils.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class TokenCreationRateLimitFilter extends OncePerRequestFilter {

    public static final String CLIENT_IP_REQUEST_ATTRIBUTE = "gateway.client.ip";

    private static final Logger logger = LoggerFactory.getLogger(TokenCreationRateLimitFilter.class);

    private final RouteResolutionService routeResolutionService;
    private final RateLimitResponseWriter rateLimitResponseWriter;

    @Value("${api-gateway-service.network.real-ip-header:X-Real-IP}")
    private String realIpHeaderName;

    public TokenCreationRateLimitFilter(RouteResolutionService routeResolutionService,
                                        RateLimitResponseWriter rateLimitResponseWriter) {
        this.routeResolutionService = routeResolutionService;
        this.rateLimitResponseWriter = rateLimitResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !routeResolutionService.isPublicRoute(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String normalizedIp;
        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, null, null);
        try {
            normalizedIp = normalizeIp(request.getHeader(realIpHeaderName));
            LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, normalizedIp, null);
            LogContext.addDetail(LogContext.STATUS, "SUCCESS");
            logger.info("ip verification succeeded");
        } catch (UnknownHostException | IllegalArgumentException e) {
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            LogContext.addDetail(LogContext.MALFORMED_IP, request.getHeader(realIpHeaderName));
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            logger.warn("ip format not valid or null");
            rateLimitResponseWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST,
                    new RateLimiterResponse(null, "ip format not valid or null")
            );
            LogContext.clear();
            return;
        }

        try {
            request.setAttribute(CLIENT_IP_REQUEST_ATTRIBUTE, normalizedIp);
            filterChain.doFilter(request, response);
        } finally {
            LogContext.clear();
        }
    }

    private String normalizeIp(String rawIp) throws UnknownHostException {
        if (rawIp == null || rawIp.isBlank()) {
            throw new IllegalArgumentException("missing mandatory X-Real-IP header");
        }
        return InetAddress.getByName(rawIp.trim()).getHostAddress();
    }
}
