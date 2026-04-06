package aidetector.apigateway.services;

import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.utils.HashUtils;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class TokenPayloadService implements TokenPayloadManager {

    private final HashUtils hashUtils;

    public TokenPayloadService(HashUtils hashUtils) {
        this.hashUtils = hashUtils;
    }

    @Override
    public TokenPayload createNewUser(TokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException{

        return new TokenPayload(UUID.randomUUID().toString(),hashUtils.hashString(reqToken.getSrcIp()));
    }

    @Override
    public Boolean verifyPayloadContext(TokenPayload payload, TokenRequest request){
        return hashUtils.verifyHash(request.getSrcIp(), payload.getHashedIp());
    }

}
