package aidetector.authentication.services;

import aidetector.authentication.model.AnonymousTokenPayload;
import aidetector.authentication.model.AnonymousTokenRequest;

import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface TokenPayloadManager {
    AnonymousTokenPayload createNewAnonymTokenPayload(AnonymousTokenRequest reqToken)
            throws InvalidKeyException,
            NoSuchAlgorithmException;
    Boolean verifyPayloadContext(AnonymousTokenPayload payload, AnonymousTokenRequest request)
            throws UnknownHostException;
}
