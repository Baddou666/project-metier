package aidetector.ratelimiter.filters;

import aidetector.ratelimiter.exceptions.InvalidTokenException;
import aidetector.ratelimiter.exceptions.RateLimitReachedException;
import aidetector.ratelimiter.model.RateLimiterResponse;
import aidetector.ratelimiter.model.VerifiedToken;
import aidetector.ratelimiter.services.RateLimitingManager;
import aidetector.ratelimiter.services.RateLimitResponseWriter;
import aidetector.ratelimiter.services.RouteResolutionService;
import aidetector.ratelimiter.services.TokenVerificationService;
import aidetector.ratelimiter.utils.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ProtectedTokenRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ProtectedTokenRateLimitFilter.class);
    public static final String IS_USER_ANONYMOUS_ATTRIBUTE = "is.user.anonym";
    private final RouteResolutionService routeResolutionService;
    private final TokenVerificationService tokenVerificationService;
    private final RateLimitingManager rateLimitingManager;
    private final RateLimitResponseWriter rateLimitResponseWriter;

    public ProtectedTokenRateLimitFilter(RouteResolutionService routeResolutionService,
                                         TokenVerificationService tokenVerificationService,
                                         RateLimitingManager rateLimitingManager,
                                         RateLimitResponseWriter rateLimitResponseWriter) {
        this.routeResolutionService = routeResolutionService;
        this.tokenVerificationService = tokenVerificationService;
        this.rateLimitingManager = rateLimitingManager;
        this.rateLimitResponseWriter = rateLimitResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !routeResolutionService.isProtectedRoute(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK, null, null);
        if (authHeader == null || authHeader.isBlank()) {
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            logger.warn("No token present");
            rateLimitResponseWriter.write(
                    response,
                    HttpStatus.BAD_REQUEST,
                    new RateLimiterResponse(null, "No token present")
            );
            LogContext.clear();
            return;
        }

        if(authHeader.startsWith("Anonym ")) {
            try {
                VerifiedToken verifiedToken = tokenVerificationService.verify(authHeader.substring(7));
                LogContext.clear();
                LogContext.setEventContext(
                        LogContext.EVENT_RATE_LIMIT_CHECK,
                        verifiedToken.userIp(),
                        verifiedToken.userId()
                );
                rateLimitingManager.verifyAttemptsLimit(verifiedToken.tokenHash());
                request.setAttribute(IS_USER_ANONYMOUS_ATTRIBUTE,true);
                filterChain.doFilter(request, response);
                if (response.getStatus() >= 200 && response.getStatus() < 300) {
                    rateLimitingManager.incrementKeyValue(verifiedToken.tokenHash());
                }
            } catch (RateLimitReachedException e) {
                LogContext.addDetail(LogContext.STATUS, "FAILED");
                logger.warn("rate limit reached for token");
                rateLimitResponseWriter.write(
                        response,
                        HttpStatus.TOO_MANY_REQUESTS,
                        new RateLimiterResponse(true, e.getMessage())
                );
            } catch (InvalidTokenException e) {
                LogContext.addDetail(LogContext.STATUS, "FAILED");
                logger.warn("token validation failed");
                rateLimitResponseWriter.write(
                        response,
                        HttpStatus.BAD_REQUEST,
                        new RateLimiterResponse(null, e.getMessage())
                );
            } finally {
                LogContext.clear();
            }
        }
        else{
            rateLimitResponseWriter.write(response,HttpStatus.NOT_IMPLEMENTED,new RateLimiterResponse(null,"Authorization Schema not implemented yet !"));
            return;
        }
    }
}
