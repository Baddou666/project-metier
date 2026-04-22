package aidetector.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "api-gateway-service.security.jwt")
@Data // Si tu utilises Lombok, sinon génère Getters/Setters
public class JwtProperties {
    //la clé secréte extraire depuis application.properties
    private String secretKey;
    //la durée maximale d'un jwt token en seconde
    private int expirationSec = 300;
    private  int minJwtSecretLength = 32;
}