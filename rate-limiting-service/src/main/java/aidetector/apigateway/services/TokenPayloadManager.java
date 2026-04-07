package aidetector.apigateway.services;

import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TokenPayloadManager {
    public TokenPayload createNewUser(TokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException;
    public Boolean verifyPayloadContext(TokenPayload payload, TokenRequest request);
}
