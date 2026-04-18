package aidetector.apigateway.filters;

import aidetector.apigateway.exceptions.RateLimitReachedException;
import aidetector.apigateway.exceptions.TokenPayloadVerificationFailed;
import aidetector.apigateway.model.AnonymousIdentification;
import aidetector.apigateway.model.ApiError;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.RateLimitingManager;
import aidetector.apigateway.services.TokenJwtManager;
import aidetector.apigateway.services.TokenPayloadManager;
import aidetector.apigateway.utils.LogContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class TokenRateLimitePerUserFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenRateLimitePerUserFilter.class);

    TokenJwtManager tokenJwtManager;
    TokenPayloadManager tokenPayloadManager;
    RateLimitingManager rateLimitingManager;
    public TokenRateLimitePerUserFilter(TokenJwtManager tokenJwtManager,
                                        TokenPayloadManager tokenPayloadManager,
                                        RateLimitingManager rateLimitingManager){
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
        this.rateLimitingManager = rateLimitingManager;
    }
    @Override
    public void doFilterInternal(@NonNull HttpServletRequest req,@NonNull HttpServletResponse res,@NonNull FilterChain chain) throws IOException, ServletException{
        String token = extractToken(req);
        LogContext.setEventContext(LogContext.EVENT_TOKEN_VERIFIED, null, null);

        if(token==null) {
            logger.debug("No token present");
            LogContext.clear();
            chain.doFilter(req,res);
            return;
        }
        try {
            TokenPayload payload = tokenJwtManager.verifyTokenAndGetPayload(token);
            LogContext.setEventContext(LogContext.EVENT_TOKEN_VERIFIED, req.getHeader("X-Real-IP"), payload.getUserId());
            logger.info("Token verified successfully");

            if(!tokenPayloadManager.verifyPayloadContext(payload,new TokenRequest(req.getHeader("X-Real-IP"))))
                throw new TokenPayloadVerificationFailed("vérification du contexte échoué");
            logger.info("Payload context verified");

            rateLimitingManager.addAttempt(payload.getUserId());
            if(rateLimitingManager.isRateLimitReached(payload.getUserId()))
                throw new RateLimitReachedException("Beaucoup de requête, merci de réessayer dans quelques instants ");

            logger.info("Rate limit check passed, setting authentication");
            SecurityContextHolder.getContext().setAuthentication(new AnonymousIdentification(payload));
            LogContext.clear();
            chain.doFilter(req,res);
        }catch(JwtException e)
        {
            LogContext.setEventContext(LogContext.EVENT_TOKEN_VERIFY_FAILED, req.getHeader("X-Real-IP"), null);
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            logger.warn("Invalid token");
            sendErrorResponse(res,HttpServletResponse.SC_UNAUTHORIZED,"token invalide: "+e.getMessage());
        }
        catch(TokenPayloadVerificationFailed e)
        {
            LogContext.addDetail(LogContext.STATUS, "PAYLOAD_VERIFICATION_FAILED");
            logger.warn("Payload verification failed");
            sendErrorResponse(res,HttpServletResponse.SC_FORBIDDEN,e.getMessage());
        }
        catch (RateLimitReachedException e)
        {
            LogContext.addDetail(LogContext.STATUS, "RATE_LIMIT_REACHED");
            logger.warn("Rate limit reached");
            sendErrorResponse(res,429,e.getMessage());
        }
        return;
    }

    private String extractToken(HttpServletRequest req)
    {
        String authHeader = req.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer "))
            return null;
        return authHeader.substring(7);
    }

    private void sendErrorResponse(HttpServletResponse res, int status, String message) throws IOException {
        LogContext.addDetail(LogContext.MESSAGE_DETAIL, message);
        logger.error("Sending error response");
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(status, message, System.currentTimeMillis());
        String json = new ObjectMapper().writeValueAsString(error);
        res.getWriter().write(json);
    }
}
