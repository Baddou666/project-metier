package aidetector.apigateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("api-gateway-service.proxyconf.headers")
@Data
public class ProxyHeaderConfig {
    private String sourceIpHeader;
}
