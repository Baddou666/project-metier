package aidetector.apigateway.controller;

import aidetector.apigateway.config.RateLimitingConfig;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.TokenJwtManager;
import aidetector.apigateway.services.TokenPayloadManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public TokenController(TokenJwtManager tokenJwtManager, TokenPayloadManager tokenPayloadManager, RateLimitingConfig rateLimitingConfig) {
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @PostMapping("/get")
    public CompletableFuture<ResponseEntity<?>> createNewToken(TokenRequest req) throws NoSuchAlgorithmException, InvalidKeyException {
        TokenPayload payload = tokenPayloadManager.createNewUser(req);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ResponseEntity.ok(Map.of("status","accepted","token",tokenJwtManager.generateNewSignedToken(payload)));
            } catch (GeneralSecurityException e) {
                return ResponseEntity.internalServerError().build();
            }
        },
                CompletableFuture.delayedExecutor(rateLimitingConfig.getTokenGenerationDelayMs(), TimeUnit.MILLISECONDS));
    }
}
