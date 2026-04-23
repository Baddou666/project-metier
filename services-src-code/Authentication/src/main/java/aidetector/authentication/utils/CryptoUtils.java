package aidetector.authentication.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.security.PrivateKey;
import java.security.PublicKey;

@Component
public class CryptoUtils {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtils.class);
    public PrivateKey extractPrivateKeyFromPem(JWK jwkPrivateKey) throws Exception {
        return switch (jwkPrivateKey){
            case RSAKey rsaKey -> rsaKey.toPrivateKey();
            case ECKey ecKey -> ecKey.toPrivateKey();
            default -> throw new IllegalArgumentException(
                    "Unsupported key type: " + jwkPrivateKey.getKeyType());
        };
    }

    public JWK createJwkKeyid(JWK jwkPublicKey) throws Exception{

        return switch (jwkPublicKey) {
            case RSAKey rsa -> new RSAKey.Builder(rsa)
                    .keyIDFromThumbprint()
                    .build();
            case ECKey ec -> new ECKey.Builder(ec)
                    .keyIDFromThumbprint()
                    .build();
            default -> throw new IllegalArgumentException(
                    "Unsupported key type: " + jwkPublicKey.getKeyType());
        };

    }
    public PublicKey getPublicKey(JWK jwkPublicKey) throws Exception{
        return switch (jwkPublicKey) {
            case RSAKey rsa -> new RSAKey.Builder(rsa)
                    .build().toPublicKey();
            case ECKey ec -> new ECKey.Builder(ec)
                    .build().toPublicKey();
            default -> throw new IllegalArgumentException(
                    "Unsupported key type: " + jwkPublicKey.getKeyType());
        };
    }
    public SignatureAlgorithm getSigAlgo(JWK key) throws Exception{
        if(key instanceof RSAKey)
            return Jwts.SIG.RS256;
        else if(key instanceof ECKey)
            return Jwts.SIG.ES256;
        else throw new IllegalArgumentException("Unsupported key type");
    }
}
