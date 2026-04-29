package aidetector.ratelimiter.services;

import aidetector.ratelimiter.exceptions.InvalidTokenSignatureException;
import aidetector.ratelimiter.model.VerifiedToken;
import aidetector.ratelimiter.utils.JwksLoader;
import aidetector.ratelimiter.utils.LogContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.util.Map;

@Service
public class TokenVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenVerificationService.class);

    private final JwksLoader jwksLoader;

    public TokenVerificationService(JwksLoader jwksLoader) {
        this.jwksLoader = jwksLoader;
    }

    public VerifiedToken verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(header -> {
                        String kid = ((JwsHeader) header).getKeyId();

                        LogContext.setEventContext(LogContext.EVENT_PUBLIC_KEY_RETREIVAL, null, null);
                        try {
                            PublicKey pub = jwksLoader.getPublicKeyByKid(kid);
                            LogContext.addDetail(LogContext.STATUS, "SUCCESS");
                            LogContext.addDetail(LogContext.PUBLIC_KEY_ID, kid);
                            logger.info("retrieved public key");
                            return pub;
                        } catch (Exception e) {
                            LogContext.addDetail(LogContext.STATUS, "FAILED");
                            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
                            logger.error("cannot retrieve the public key !");
                            throw new IllegalStateException("cannot retrieve the public key !", e);
                        } finally {
                            LogContext.clear();
                        }
                    })
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Map<?, ?> userInfo = claims.get("userInfo", Map.class);
            String userId = userInfo == null ? null : String.valueOf(userInfo.get("userId"));
            String userIp = userInfo == null ? null : String.valueOf(userInfo.get("userIp"));
            LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK, userIp, userId);
            LogContext.addDetail(LogContext.STATUS, "SUCCESS");
            logger.info("token verified successfully");
            return new VerifiedToken(hashToken(token), userId, userIp);
        } catch (JwtException | IllegalStateException e) {
            LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK, null, null);
            LogContext.addDetail(LogContext.TOKEN_SIGNATURE, "no match");
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            throw new InvalidTokenSignatureException("signature vérification failed");
        }
    }

    private String hashToken(String token) {
        return DigestUtils.sha256Hex(token);
    }
}
