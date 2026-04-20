package aidetector.apigateway.controller;

import aidetector.apigateway.config.ProxyHeaderConfig;
import aidetector.apigateway.model.AnonymousIdentification;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.utils.LogContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class DetectController {

    private static final Logger logger = LoggerFactory.getLogger(DetectController.class);

    @Value("${api-gateway-service.next-service.base-url}")
    private String targetBaseUrl;

    @Value("${api-gateway-service.next-service.port}")
    private String targetPort;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ProxyHeaderConfig proxyHeaderConfig;

    public DetectController(ProxyHeaderConfig proxyHeaderConfig) {
        this.proxyHeaderConfig = proxyHeaderConfig;
    }


    // l'endpoint doit nécessairement commencer par un '/'
    public final static String detectEndpoint = "/api/detect";


    @PostMapping(detectEndpoint)
    public ResponseEntity<String> detect(@RequestBody String payload, HttpServletRequest req, AnonymousIdentification auth) {
        String srcIp = req.getHeader(proxyHeaderConfig.getSourceIpHeader());
        String userId = null;
        
        if (auth != null && auth.getPrincipal() instanceof TokenPayload) {
            userId = ((TokenPayload) auth.getPrincipal()).getUserId();
        }

        LogContext.setEventContext(LogContext.EVENT_FORWARD_REQUEST, srcIp, userId);

        String targetUrl =
                (targetBaseUrl.endsWith("/") ?
                        targetBaseUrl.substring(0,targetBaseUrl.length() - 1) : targetBaseUrl)
                + (StringUtils.hasText(targetPort) ?
                        ":" + targetPort : "")
                + detectEndpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> requestEntity = new HttpEntity<>(payload, headers);

        try {
            logger.info("Forwarding request to target service");
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            LogContext.addDetail(LogContext.STATUS, "SUCCESS");
            logger.info("Request forwarded successfully");
            return response;
            
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            LogContext.addDetail(LogContext.STATUS, "HTTP_ERROR");
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            logger.warn("Target service returned HTTP error");
            // Forward the exact error response from the target service
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsString());
        } catch (Exception e) {
            LogContext.addDetail(LogContext.STATUS, "ERROR");
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            logger.error("Error forwarding request", e);
            return ResponseEntity.internalServerError().body("Error forwarding request: " + e.getMessage());
        } finally {
            LogContext.clearTemporary();
        }
    }
}
