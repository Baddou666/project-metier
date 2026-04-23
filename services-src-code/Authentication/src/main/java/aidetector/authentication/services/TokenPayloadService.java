package aidetector.authentication.services;

import aidetector.authentication.model.AnonymousTokenPayload;
import aidetector.authentication.model.AnonymousTokenRequest;
import aidetector.authentication.utils.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class TokenPayloadService implements TokenPayloadManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenPayloadService.class);

    @Override
    public AnonymousTokenPayload createNewAnonymTokenPayload(AnonymousTokenRequest reqToken) throws InvalidKeyException, NoSuchAlgorithmException{
        String userId = UUID.randomUUID().toString();
        String userIp = reqToken.getSrcIp();
        return new AnonymousTokenPayload(userId, userIp);
    }

    @Override
    public Boolean verifyPayloadContext(AnonymousTokenPayload payload, AnonymousTokenRequest request) {
        LogContext.setEventContext(LogContext.EVENT_PAYLOAD_CONTEXT_VERIFY, request.getSrcIp(), payload.getUserId());

        try {
            InetAddress payloadIp = InetAddress.getByName(payload.getUserIp());
            InetAddress requestIp = InetAddress.getByName(request.getSrcIp());
            Boolean verified = payloadIp.equals(requestIp);
            LogContext.addDetail(LogContext.STATUS, verified ? "SUCCESS" : "FAILED");
            if (verified) {
                logger.info("Payload context verified");
            } else {
                logger.warn("Payload context verification failed");
            }
            return verified;
        }catch (UnknownHostException e){
            return false;
        }
    }

}
