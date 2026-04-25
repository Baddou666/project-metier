package aidetector.authentication.controller;

import aidetector.authentication.config.AnonymJwtProperties;
import aidetector.authentication.exceptions.InvalidConfigException;
import aidetector.authentication.exceptions.TokenGenerationFailedException;
import aidetector.authentication.model.AnonymousTokenPayload;
import aidetector.authentication.model.AnonymousTokenRequest;
import aidetector.authentication.model.AuthApiResponse;
import aidetector.authentication.services.TokenGenerationRateLimitManager;
import aidetector.authentication.services.TokenJwtManager;
import aidetector.authentication.services.TokenPayloadManager;
import aidetector.authentication.utils.LogContext;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/anonym-token")
public class AnonymousTokenController {

    private static final Logger logger = LoggerFactory.getLogger(AnonymousTokenController.class);

    private final TokenJwtManager tokenJwtManager;
    private final TokenPayloadManager tokenPayloadManager;
    private final TokenGenerationRateLimitManager tokenGenerationRateLimitManager;
    private final AnonymJwtProperties jwtProperties;

    @Value("${api-gateway-service.network.forwarded-client-ip-header}")
    private String forwardedClientIpHeaderName;

    @Value("${api-gateway-service.security.authentication.gateway-shared-secret-header}")
    private String gatewayAuthHeaderName;

    @Value("${api-gateway-service.security.authentication.gateway-shared-secret}")
    private String gatewayAuthSharedSecret;

    public AnonymousTokenController(TokenJwtManager tokenJwtManager,
                                    TokenPayloadManager tokenPayloadManager,
                                    TokenGenerationRateLimitManager tokenGenerationRateLimitManager, AnonymJwtProperties jwtProperties) {
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
        this.tokenGenerationRateLimitManager = tokenGenerationRateLimitManager;
        this.jwtProperties = jwtProperties;
    }
    @PostConstruct
    public void validateHeadersConfig(){
        if(this.forwardedClientIpHeaderName == null ||
                this.gatewayAuthHeaderName == null ||
                this.gatewayAuthSharedSecret == null){
            throw new InvalidConfigException("one or more of header config parameter is null");
        }
    }

    @PostMapping("/get")
    public Object createNewToken(HttpServletRequest request)
            throws NoSuchAlgorithmException, InvalidKeyException {
        if (!gatewayAuthSharedSecret.equals(request.getHeader(gatewayAuthHeaderName))) {
            int status = HttpStatus.FORBIDDEN.value();
            LogContext.setEventContext(LogContext.EVENT_GATEWAY_AUTH, null, null);
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            LogContext.addDetail(LogContext.EXCEPTION_MSG, "request rejected: invalid gateway signature");
            logger.warn("request rejected: invalid gateway signature");
            LogContext.clear();
            return ResponseEntity.status(status)
                    .body(new AuthApiResponse(status, "forbidden: gateway signature invalid", null));
        }

        AnonymousTokenRequest tokenRequest = new AnonymousTokenRequest(request.getHeader(forwardedClientIpHeaderName));
        if (tokenRequest.getSrcIp() == null || tokenRequest.getSrcIp().isBlank()) {
            int status = HttpStatus.BAD_REQUEST.value();
            LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, null, null);
            LogContext.addDetail(LogContext.STATUS, "FAILED");
            LogContext.addDetail(LogContext.EXCEPTION_MSG, "missing forwarded client ip header");
            logger.warn("missing forwarded client ip header");
            LogContext.clear();
            return ResponseEntity.status(status)
                    .body(new AuthApiResponse(status, "missing forwarded client ip header", null));
        }

        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, null, null);
        try {
            InetAddress ipObj = InetAddress.getByName(tokenRequest.getSrcIp());
            tokenRequest.setSrcIp(ipObj.getHostAddress());
        } catch (UnknownHostException e) {
            String message = "provided ip invalid";
            int status = 422;
            LogContext.addDetail(LogContext.MALFORMED_IP, tokenRequest.getSrcIp());
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            LogContext.addDetail(LogContext.STATUS, status);
            logger.warn(message);
            LogContext.clear();
            return ResponseEntity.status(status).body(new AuthApiResponse(status, message, null));
        }

        try {
            LogContext.clear();
            LogContext.setEventContext(LogContext.EVENT_TOKEN_GENERATION_PERMISSION, tokenRequest.getSrcIp(), null);
            if (tokenGenerationRateLimitManager.isMaxTokenPerIpReached(tokenRequest.getSrcIp())) {
                int status = HttpStatus.TOO_MANY_REQUESTS.value();
                LogContext.addDetail(LogContext.STATUS, "FAILED");
                logger.warn("max allowed token reached");
                LogContext.clear();
                return ResponseEntity.status(status).body(new AuthApiResponse(status, "max allowed token reached !", null));
            }

            AnonymousTokenPayload payload = tokenPayloadManager
                    .createNewAnonymTokenPayload(new AnonymousTokenRequest(tokenRequest.getSrcIp()));
            LogContext.clear();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    String token = tokenJwtManager.generateNewSignedToken(payload);
                    tokenGenerationRateLimitManager.addTokenToIp(payload.getUserIp());
                    LogContext.addDetail(LogContext.STATUS, "SUCCESS");
                    logger.info("Token generated successfully");
                    int status = HttpServletResponse.SC_ACCEPTED;
                    LogContext.clear();
                    return ResponseEntity.status(status).body(new AuthApiResponse(status, "allowed", token));
                } catch (TokenGenerationFailedException e) {
                    LogContext.addDetail(LogContext.STATUS, "ERROR");
                    LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
                    logger.error("Token generation failed", e);
                    return ResponseEntity.internalServerError().body(new AuthApiResponse(500, e.getMessage(), null));
                }
            }, CompletableFuture.delayedExecutor(jwtProperties.getGenerationDelayMs(), TimeUnit.MILLISECONDS));
        } finally {
            LogContext.clear();
        }
    }
}
