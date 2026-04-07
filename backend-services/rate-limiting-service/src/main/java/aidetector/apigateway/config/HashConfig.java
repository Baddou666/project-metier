package aidetector.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "api-gateway-service.security.hash")
@Data
public class HashConfig {
    private String algorithme;
    private String salt;
}
