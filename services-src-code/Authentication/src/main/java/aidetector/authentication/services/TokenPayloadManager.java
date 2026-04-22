package aidetector.authentication.services;

import aidetector.authentication.model.TokenPayload;
import aidetector.authentication.model.TokenRequest;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TokenPayloadManager {
    public TokenPayload createNewUser(TokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException;
    public Boolean verifyPayloadContext(TokenPayload payload, TokenRequest request);
}
