package aidetector.ratelimiter.utils;
import aidetector.ratelimiter.controller.RateLimitingController;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.Key;
import java.security.PublicKey;
import java.util.List;
@Component
public class JwksLoader {

    private final JWKSource<SecurityContext> jwkSource;

    private static final Logger logger = LoggerFactory.getLogger(JwksLoader.class);

    public JwksLoader(@Value("${api-gateway-service.security.auth-sevice.url}") String jwksUrl) throws Exception {

        // Limites : Timeout de connexion (2s), Timeout de lecture (2s), Limite de taille (50ko)
        DefaultResourceRetriever retriever =
                new DefaultResourceRetriever(2000, 2000, 51200);

        this.jwkSource = JWKSourceBuilder.create(URI.create(jwksUrl).toURL(), retriever)
                .build();

        // 🔥 FAIL FAST : Récupère les clés pour s'assurer que l'URL est joignable au démarrage
        LogContext.setEventContext(LogContext.EVENT_PUBLIC_KEY_RETREIVAL,null,null);
        for(int i = 0; i < 10 ;i++) {
            try{
                logger.info("attempting to retrieve the public key from the auth server");
                JWKSelector selector = new JWKSelector(new JWKMatcher.Builder().build());
                jwkSource.get(selector, null);
                break;
            }catch (KeySourceException e){
                LogContext.addDetail(LogContext.EXCEPTION_MSG,e.getMessage());
                logger.error("failed fetching for the public key, trying again...");
                LogContext.clearTemporary();
                if(i == 9)
                    throw new KeySourceException(e);
                Thread.sleep(2000);
            }
        }
        LogContext.addDetail(LogContext.STATUS,"SUCCESS");
        logger.info("the public key retrieved successfully");
        LogContext.clear();
    }

    public PublicKey getPublicKeyByKid(String kid) throws Exception {

        // 1. Créer un filtre pour chercher le "kid" précis
        JWKMatcher matcher = new JWKMatcher.Builder()
                .keyID(kid)
                .build();

        JWKSelector selector = new JWKSelector(matcher);

        // 2. Interroger la source (le cache ou l'URL distante)
        List<JWK> keys = jwkSource.get(selector, null);

        // 3. Vérifier si on a trouvé la clé
        if (keys == null || keys.isEmpty()) {
            throw new IllegalArgumentException("Aucune clé trouvée pour le KID : " + kid);
        }

        // 4. Récupérer la première clé correspondante
        JWK jwk = keys.getFirst();

        if (jwk instanceof RSAKey) {
            return jwk.toRSAKey().toPublicKey();
        }
         else if (jwk instanceof ECKey) {
             return jwk.toECKey().toPublicKey();
         }

        throw new UnsupportedOperationException("Type de clé non supporté : " + jwk.getKeyType());
    }
}