package aidetector.authentication.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
@ConfigurationProperties(prefix = "api-gateway-service.security.authentication.anonym-jwt")
@NoArgsConstructor// Si tu utilises Lombok, sinon génère Getters/Setters
public class AnonymJwtProperties {
    //la clé secréte extraire depuis application.properties
    //la durée maximale d'un jwt token en seconde
    @Setter
    @Getter
    private int expirationSec = 300;
    @Getter
    @Setter
    private Long generationDelayMs;

    @Setter
    @Getter
    private Resource privateKeyResource;

    /**
     * Extrait directement le contenu de la clé privée sous forme de String.
     */
    public String getPrivatePemFileContent() {
        if (privateKeyResource == null) {
            throw new IllegalStateException("La ressource privateKeyResource n'est pas configurée.");
        }
        try (InputStream is = privateKeyResource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire le fichier de la clé privée", e);
        }
    }
}