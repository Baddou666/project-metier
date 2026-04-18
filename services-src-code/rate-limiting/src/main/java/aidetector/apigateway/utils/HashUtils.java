package aidetector.apigateway.utils;

import aidetector.apigateway.config.HashConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class HashUtils {

    private static final Logger logger = LoggerFactory.getLogger(HashUtils.class);

    private final Mac hashAlgo;
    private final String hashSalt;

    @Autowired
    public HashUtils(HashConfig hashConfig)throws NoSuchAlgorithmException, IllegalArgumentException {

        this.hashAlgo = Mac.getInstance(hashConfig.getAlgorithme());
        this.hashSalt = hashConfig.getSalt();
        if(this.hashSalt.length() < hashConfig.getMinSaltLength() || this.hashSalt.isBlank()){
            throw new IllegalArgumentException("the hash salt must be provided and must be at least 16 character long !");
        }
        logger.info("HashUtils initialized");
    }

    /**
     * @ permet de calculer le hash unique sha256 d'un string
     * @param src la chaîne source à hasher
     * @return le hash sha256 de src
     */
    public String hashString(String src) throws NoSuchAlgorithmException, InvalidKeyException {
        LogContext.addDetail(LogContext.EVENT_TYPE, LogContext.EVENT_HASH_COMPUTE);
        SecretKeySpec secretKeySpec = new SecretKeySpec(this.hashSalt.getBytes(StandardCharsets.UTF_8),hashAlgo.getAlgorithm());

            hashAlgo.init(secretKeySpec);
            byte[] hashTab = hashAlgo.doFinal(src.getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getEncoder().encodeToString(hashTab);
            logger.debug("Hash generated successfully");
            return hash;
    }
    public Boolean verifyHash(String data, String expectedHash){
        LogContext.addDetail(LogContext.EVENT_TYPE, LogContext.EVENT_HASH_VERIFY);
        try {
            String actualHash = hashString(data);
            Boolean verified = MessageDigest.isEqual(actualHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
            logger.debug("Hash verification result: {}", verified);
            return verified;
        }
        catch (GeneralSecurityException e) {
            LogContext.addDetail(LogContext.EXCEPTION_MSG, e.getMessage());
            logger.error("Hash verification failed");
            return false;
        }
    }
}
