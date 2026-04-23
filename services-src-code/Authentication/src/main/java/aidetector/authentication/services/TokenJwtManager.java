package aidetector.authentication.services;

import aidetector.authentication.model.AnonymousTokenPayload;
import com.nimbusds.jose.jwk.JWK;
import io.jsonwebtoken.JwtException;

import java.security.GeneralSecurityException;

public interface TokenJwtManager {
    public String generateNewSignedToken(AnonymousTokenPayload client);
    //public AnonymousTokenPayload verifyTokenAndGetPayload(String token) throws JwtException, IllegalArgumentException;
    public JWK getJwkPublicKeyWithId();
}
