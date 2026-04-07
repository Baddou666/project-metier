package aidetector.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties("api-gateway-service.security.ratelimiting")
@Data
public class RateLimitingConfig {
    private Long maxPerMinAttempts;
    private final Long windowSize = 60L;
    private Long tokenGenerationDelayMs;
}
