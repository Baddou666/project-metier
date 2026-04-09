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
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class TokenRateLimitePerUserFilter extends OncePerRequestFilter {

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
        if(token==null) {
            chain.doFilter(req,res);
            return;
        }
        try {
            TokenPayload payload = tokenJwtManager.verifyTokenAndGetPayload(token);
            if(!tokenPayloadManager.verifyPayloadContext(payload,new TokenRequest(req.getHeader("X-Real-IP"))))
                throw new TokenPayloadVerificationFailed("vérification du contexte échoué");
            rateLimitingManager.addAttempt(payload.getUserId());
            if(rateLimitingManager.isRateLimitReached(payload.getUserId()))
                throw new RateLimitReachedException("Beaucoup de requête, merci de réessayer dans quelques instants ");

            SecurityContextHolder.getContext().setAuthentication(new AnonymousIdentification(payload));
            chain.doFilter(req,res);
        }catch(JwtException e)
        {
            //res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"token invalide: "+e.getMessage());
            sendErrorResponse(res,HttpServletResponse.SC_UNAUTHORIZED,"token invalide: "+e.getMessage());
        }
        catch(TokenPayloadVerificationFailed e)
        {
            //res.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
            sendErrorResponse(res,HttpServletResponse.SC_FORBIDDEN,e.getMessage());
        }
        catch (RateLimitReachedException e)
        {
            //res.sendError(429,e.getMessage());
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
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(status, message, System.currentTimeMillis());
        String json = new ObjectMapper().writeValueAsString(error);
        res.getWriter().write(json);
    }
}
