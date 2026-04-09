package aidetector.apigateway;

import aidetector.apigateway.config.HashConfig;
import aidetector.apigateway.utils.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
class HashUtilsTest {

    private HashUtils hashUtils;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        HashConfig config = new HashConfig();
        config.setSalt("mon_salt_secret_16_mi");
        config.setAlgorithme("HmacSHA256");

        hashUtils = new HashUtils(config);
    }

    @Test
    void testHashConsistency() throws NoSuchAlgorithmException, InvalidKeyException {
        String ip = "1.2.3.4";

        String hash1 = hashUtils.hashString(ip);
        String hash2 = hashUtils.hashString(ip);
        System.out.println("hash ip '"+ip+"' est : '"+hash1+"'");
        assertEquals(hash1, hash2, "Le même IP doit toujours donner le même Hash");
        assertNotEquals(ip, hash1, "Le hash ne doit pas être égal à l'IP en clair");
    }
}
