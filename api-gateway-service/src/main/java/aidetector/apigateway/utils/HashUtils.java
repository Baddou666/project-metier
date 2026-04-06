package aidetector.apigateway.utils;

import aidetector.apigateway.config.HashConfig;
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

    private final HashConfig hashConfig;

    public HashUtils(HashConfig hashConfig) {
        this.hashConfig = hashConfig;
    }

    /**
     * @ permet de calculer le hash unique sha256 d'un string
     * @param src la chaîne source à hasher
     * @return le hash sha256 de src
     */
    public String hashString(String src) throws NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec secretKeySpec = new SecretKeySpec(hashConfig.getSalt().getBytes(StandardCharsets.UTF_8),hashConfig.getAlgorithme());

            Mac mac = Mac.getInstance(hashConfig.getAlgorithme());
            mac.init(secretKeySpec);
            byte[] hashTab = mac.doFinal(src.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashTab);
    }
    public Boolean verifyHash(String data, String expectedHash){

        try {
            String actualHash = hashString(data);
            return MessageDigest.isEqual(actualHash.getBytes(StandardCharsets.UTF_8), expectedHash.getBytes(StandardCharsets.UTF_8));
        }
        catch (GeneralSecurityException e) {
            return false;
        }
    }
}
