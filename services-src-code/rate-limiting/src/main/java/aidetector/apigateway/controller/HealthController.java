package aidetector.apigateway.controller;

import aidetector.apigateway.utils.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/api/health")
    public Map<String, String> healthCheck() {
        LogContext.setEventContext("HEALTH_CHECK", null, null);
        try {
            logger.info("Health check endpoint called");
            return Map.of("status", "UP");
        } finally {
            LogContext.clear();
        }
    }
}
