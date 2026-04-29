package aidetector.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("api-gateway-service.security.authentication.token-rate-limit")
@Data
public class TokenGenerationRateLimitConfig {
    private Long maxTokenPerIp = 15L;
    private Long windowSizePerIp = 60L;
}
