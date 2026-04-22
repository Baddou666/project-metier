package aidetector.authentication.services;

import aidetector.authentication.model.TokenPayload;
import io.jsonwebtoken.JwtException;

import java.security.GeneralSecurityException;

public interface TokenJwtManager {
    public String generateNewSignedToken(TokenPayload client) throws GeneralSecurityException;
    public TokenPayload verifyTokenAndGetPayload(String token) throws JwtException, IllegalArgumentException;
    }
