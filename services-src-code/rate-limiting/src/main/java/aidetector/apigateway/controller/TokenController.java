package aidetector.apigateway.controller;

import aidetector.apigateway.config.ProxyHeaderConfig;
import aidetector.apigateway.config.RateLimitingConfig;
import aidetector.apigateway.model.AnonymousIdentification;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.RateLimitingManager;
import aidetector.apigateway.services.TokenJwtManager;
import aidetector.apigateway.services.TokenPayloadManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    private final TokenJwtManager tokenJwtManager;
    private final TokenPayloadManager tokenPayloadManager;
    private final RateLimitingConfig rateLimitingConfig;
    private final ProxyHeaderConfig proxyHeaderConfig;
    private final RateLimitingManager rateLimitingManager;

    public TokenController(TokenJwtManager tokenJwtManager,
                           TokenPayloadManager tokenPayloadManager,
                           RateLimitingConfig rateLimitingConfig,
                           ProxyHeaderConfig proxyHeaderConfig, RateLimitingManager rateLimitingManager) {
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
        this.rateLimitingConfig = rateLimitingConfig;
        this.proxyHeaderConfig = proxyHeaderConfig;
        this.rateLimitingManager = rateLimitingManager;
    }

    @PostMapping("/get")
    public Object createNewToken(HttpServletRequest req, AnonymousIdentification auth) throws NoSuchAlgorithmException, InvalidKeyException {

        if(auth != null && auth.getPrincipal() != null){
            return ResponseEntity.ok(Map.of("status","bad request","message","you have already a valide token"));
        }
        String srcIp = req.getHeader(proxyHeaderConfig.getSourceIpHeader());
        TokenPayload payload = tokenPayloadManager.createNewUser(new TokenRequest(srcIp));
        return CompletableFuture.supplyAsync(() -> {
            try {
                rateLimitingManager.addToken(srcIp);
                return ResponseEntity.ok(Map.of("status","accepted","token",tokenJwtManager.generateNewSignedToken(payload)));
            } catch (GeneralSecurityException e) {
                return ResponseEntity.internalServerError().build();
            }
        },
                CompletableFuture.delayedExecutor(rateLimitingConfig.getTokenGenerationDelayMs(), TimeUnit.MILLISECONDS));
    }
    @GetMapping("/test")
    public String testAccess(){
        return "Ok";
    }
}
