package aidetector.apigateway.controller;

import aidetector.apigateway.config.ProxyHeaderConfig;
import aidetector.apigateway.config.RateLimitingConfig;
import aidetector.apigateway.model.AnonymousIdentification;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.RateLimitingManager;
import aidetector.apigateway.services.TokenJwtManager;
import aidetector.apigateway.services.TokenPayloadManager;
import aidetector.apigateway.utils.LogContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

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
        String srcIp = req.getHeader(proxyHeaderConfig.getSourceIpHeader());
        LogContext.setEventContext(LogContext.EVENT_TOKEN_REQUEST, srcIp, null);

        try {
            if (auth != null && auth.getPrincipal() != null) {
                LogContext.addDetail(LogContext.STATUS, "DENIED_EXISTING_TOKEN");
                logger.info("Token request denied: User already has valid token");
                return ResponseEntity.ok(Map.of("status", "bad request", "message", "you have already a valide token"));
            }

            TokenPayload payload = tokenPayloadManager.createNewUser(new TokenRequest(srcIp));
            LogContext.addDetail(LogContext.USER_ID, payload.getUserId());

            return CompletableFuture.supplyAsync(() -> {
                try {
                    rateLimitingManager.addToken(srcIp);
                    Long tokenCount = rateLimitingManager.getTokenPerIp(srcIp);
                    LogContext.addDetail(LogContext.TOKEN_COUNT, tokenCount);
                    logger.info("Token counter incremented");

                    String token = tokenJwtManager.generateNewSignedToken(payload);
                    LogContext.addDetail(LogContext.STATUS, "SUCCESS");
                    logger.info("Token generated successfully");
                    return ResponseEntity.ok(Map.of("status", "accepted", "token", token));
                } catch (GeneralSecurityException e) {
                    LogContext.addDetail(LogContext.STATUS, "ERROR");
                    LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
                    logger.error("Token generation failed", e);
                    return ResponseEntity.internalServerError().build();
                }
            }, CompletableFuture.delayedExecutor(rateLimitingConfig.getTokenGenerationDelayMs(), TimeUnit.MILLISECONDS));
        } finally {
            LogContext.clearTemporary();
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyToken() {
        return ResponseEntity.ok(Map.of("status", "valid"));
    }
}
