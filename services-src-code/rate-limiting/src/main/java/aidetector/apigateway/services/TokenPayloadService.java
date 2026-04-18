package aidetector.apigateway.services;

import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.utils.HashUtils;
import aidetector.apigateway.utils.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class TokenPayloadService implements TokenPayloadManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenPayloadService.class);

    private final HashUtils hashUtils;

    public TokenPayloadService(HashUtils hashUtils) {
        this.hashUtils = hashUtils;
    }

    @Override
    public TokenPayload createNewUser(TokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException{
        String userId = UUID.randomUUID().toString();
        String hashedIp = hashUtils.hashString(reqToken.getSrcIp());
        LogContext.setEventContext(LogContext.EVENT_TOKEN_REQUEST, reqToken.getSrcIp(), userId);
        logger.info("New user payload created");
        return new TokenPayload(userId, hashedIp);
    }

    @Override
    public Boolean verifyPayloadContext(TokenPayload payload, TokenRequest request){
        LogContext.setEventContext(LogContext.EVENT_PAYLOAD_CONTEXT_VERIFY, request.getSrcIp(), payload.getUserId());
        Boolean verified = hashUtils.verifyHash(request.getSrcIp(), payload.getHashedIp());
        LogContext.addDetail(LogContext.STATUS, verified ? "SUCCESS" : "FAILED");
        if (verified) {
            logger.info("Payload context verified");
        } else {
            logger.warn("Payload context verification failed");
        }
        return verified;
    }

}
