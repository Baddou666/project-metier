package aidetector.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("api-gateway-service.data.redis")
@Data
public class RedisConfig {

    private String redisHost;


}
