package aidetector.apigateway;

import aidetector.apigateway.config.HashConfig;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.TokenPayloadService;
import aidetector.apigateway.utils.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TokenPayloadTest {

    private TokenPayloadService tokenPayloadService;

    @BeforeEach
    void setUp() throws Exception {
        HashConfig config = new HashConfig();
        config.setSalt("mon_salt_secret123");
        config.setAlgorithme("HmacSHA256");
        this.tokenPayloadService = new TokenPayloadService(new HashUtils(config));

    }

    @Test
    void testHashConsistency() throws Exception {
        TokenPayload tokenPayload = tokenPayloadService.createNewUser(new TokenRequest("102.65.47.9"));
        TokenPayload tokenPayload1 = tokenPayloadService.createNewUser(new TokenRequest("102.65.47.9"));
        assertNotEquals(tokenPayload1.getUserId(), tokenPayload.getUserId(),"les ids des users ne doivent jamais être identiques");
        assertEquals(false, tokenPayloadService.verifyPayloadContext(tokenPayload,new TokenRequest("102.55.47.9")),"la vérifs contexte doit échouer");
        System.out.println(tokenPayload.getHashedIp());
        System.out.println(tokenPayload.getUserId());

    }
}
