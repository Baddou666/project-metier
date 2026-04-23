package aidetector.authentication.controller;


import aidetector.authentication.exceptions.TokenGenerationFailedException;
import aidetector.authentication.model.AnonymousTokenPayload;
import aidetector.authentication.model.AnonymousTokenRequest;
import aidetector.authentication.model.AuthApiResponse;
import aidetector.authentication.services.TokenJwtManager;
import aidetector.authentication.services.TokenPayloadManager;
import aidetector.authentication.utils.LogContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/anonym-token")
public class AnonymousTokenController {

    private static final Logger logger = LoggerFactory.getLogger(AnonymousTokenController.class);

    private final TokenJwtManager tokenJwtManager;
    private final TokenPayloadManager tokenPayloadManager;

    @Value("${api-gateway-service.security.authentication.token.generation-delay-ms}")
    private Long tokenGenerationDelayMs;

    @Value("${api-gateway-service.security.authentication.token.permission-server-url}")
    private String permissionServerUrl;

    public AnonymousTokenController(TokenJwtManager tokenJwtManager,
                                    TokenPayloadManager tokenPayloadManager) {
        this.tokenJwtManager = tokenJwtManager;
        this.tokenPayloadManager = tokenPayloadManager;
    }

    @PostMapping("/get")
    public Object createNewToken(@RequestBody AnonymousTokenRequest tokenRequest) throws NoSuchAlgorithmException, InvalidKeyException {

        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION,null,null);
        try {
            InetAddress ipObj = InetAddress.getByName(tokenRequest.getSrcIp());
            tokenRequest.setSrcIp(ipObj.getHostAddress());
        } catch(UnknownHostException e){
            String message = "provided ip invalid";
            int status = 422; //donnée non traitable !!
            LogContext.addDetail(LogContext.MALFORMED_IP,tokenRequest.getSrcIp());
            LogContext.addDetail(LogContext.EXCEPTION_MSG,e.getMessage());
            LogContext.addDetail(LogContext.STATUS,status);
            logger.warn(message);
            LogContext.clear();
            return ResponseEntity.status(status).body(new AuthApiResponse(status,message,null));
        }

        try {
            AnonymousTokenPayload payload = tokenPayloadManager.createNewAnonymTokenPayload(new AnonymousTokenRequest(tokenRequest.getSrcIp()));
            LogContext.clear();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    LogContext.setEventContext(LogContext.EVENT_TOKEN_GENERATION_PERMISSION,payload.getUserIp(),payload.getUserId());
                    logger.info("acquiring token generation permission");
                    getTokenGenerationPermission(tokenRequest.getSrcIp());
                    logger.info("token generation permission acquired");
                    String token = tokenJwtManager.generateNewSignedToken(payload);
                    LogContext.addDetail(LogContext.STATUS, "SUCCESS");
                    logger.info("Token generated successfully");
                    int status =HttpServletResponse.SC_ACCEPTED;
                    LogContext.clear();
                    return ResponseEntity.status(status).body(new AuthApiResponse(status,"allowed",token));
                } catch (TokenGenerationFailedException e){
                    LogContext.addDetail(LogContext.STATUS, "ERROR");
                    LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
                    logger.error("Token generation failed", e);
                    return ResponseEntity.internalServerError().body(new AuthApiResponse(500,e.getMessage(),null));
                }
            }, CompletableFuture.delayedExecutor(tokenGenerationDelayMs, TimeUnit.MILLISECONDS));
        }
        finally {
            LogContext.clear();
        }
    }



// ... dans ta classe (qui doit être un @Component ou un @Service)



    // Instanciation directe pour faire simple (tu peux aussi l'injecter si tu as un Bean)
    private final RestTemplate restTemplate = new RestTemplate();

    private void getTokenGenerationPermission(String srcIp) {
        // Construction de l'URL finale, ex: http://monservice:8080/api/check?ip=192.168.1.1
        try {
            // Fait un appel GET. Si le serveur répond 200 OK, ça passe à la suite.
            restTemplate.postForEntity(permissionServerUrl, Map.of("srcIp",srcIp), Void.class);

        } catch (Exception e) {
            // Cette exception attrape TOUT :
            // - Le service est éteint ou injoignable (ResourceAccessException)
            // - Le service répond un 403 Forbidden ou 400 Bad Request (HttpClientErrorException)
            // - Le service plante avec un 500 (HttpServerErrorException)
            LogContext.addDetail(LogContext.STATUS,"FAILED");
            LogContext.addDetail(LogContext.EXCEPTION_MSG,e.getMessage());
            logger.warn("error acquiring the permission to create new token");
            LogContext.clear();
            throw new TokenGenerationFailedException("Le service de permission ne répond pas ou a refusé l'accès pour l'IP : " + srcIp);
        }
    }

}
