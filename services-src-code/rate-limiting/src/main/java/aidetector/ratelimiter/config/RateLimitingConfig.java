package aidetector.ratelimiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties("api-gateway-service.security.ratelimiting")
@Data
public class RateLimitingConfig {
    private Long maxAttemptsPerToken = 100L;
    private Long windowSizePerToken = 60L;
    private Long tokenGenerationDelayMs = 200L;
}
