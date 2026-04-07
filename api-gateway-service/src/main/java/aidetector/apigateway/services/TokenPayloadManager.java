package aidetector.apigateway.services;

import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import org.springframework.stereotype.Component;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TokenPayloadManager {
    public TokenPayload createNewUser(TokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException;
    public Boolean verifyPayloadContext(TokenPayload payload, TokenRequest request);
}
