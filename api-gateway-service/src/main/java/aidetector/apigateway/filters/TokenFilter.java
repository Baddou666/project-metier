package aidetector.apigateway.filters;

import aidetector.apigateway.exceptions.RateLimitReachedException;
import aidetector.apigateway.exceptions.TokenNotProvidedException;
import aidetector.apigateway.exceptions.TokenPayloadVerificationFailed;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class TokenFilter extends OncePerRequestFilter {

    TokenJwtManager tokenJwtManager;
    TokenPayloadManager tokenPayloadManager;
    RateLimitingManager rateLimitingManager;
    public TokenFilter(TokenJwtManager tokenJwtManager,
                       TokenPayloadManager tokenPayloadManager,
                       RateLimitingManager rateLimitingManager){
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
        this.rateLimitingManager = rateLimitingManager;
    }
    @Override
    public void doFilterInternal(@NonNull HttpServletRequest req,@NonNull HttpServletResponse res,@NonNull FilterChain chain) throws IOException, ServletException{
        String token = extractToken(req);

        try {
            TokenPayload payload = tokenJwtManager.verifyTokenAndGetPayload(token);
            if(tokenPayloadManager.verifyPayloadContext(payload,new TokenRequest(req.getHeader("X-Real-IP"))))
                throw new TokenPayloadVerificationFailed("vérification du contexte échoué");
            rateLimitingManager.addAttempt(payload.getUserId());
            if(rateLimitingManager.isRateLimitReached(payload.getUserId()))
                throw new RateLimitReachedException("Beaucoup de requête, merci de réessayer dans quelques instants ");
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(payload,null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req,res);
        }catch(JwtException e)
        {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED,"token invalide: "+e.getMessage());
        }
        catch(TokenPayloadVerificationFailed e)
        {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,e.getMessage());
        }
        catch (RateLimitReachedException e)
        {
            res.sendError(429,e.getMessage());
        }
        return;
    }

    private String extractToken(HttpServletRequest req) throws TokenNotProvidedException
    {
        String authHeader = req.getHeader("Authorization");
        if(authHeader == null || !authHeader.startsWith("Bearer "))
            throw new TokenNotProvidedException("Token not provided");
        return authHeader.substring(7);
    }
}
