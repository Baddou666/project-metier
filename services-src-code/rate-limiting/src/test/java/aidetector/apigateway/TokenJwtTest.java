package aidetector.apigateway;

import aidetector.apigateway.config.HashConfig;
import aidetector.apigateway.config.JwtProperties;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.TokenJwtService;
import aidetector.apigateway.services.TokenPayloadService;
import aidetector.apigateway.utils.HashUtils;
import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenJwtTest {

    private TokenJwtService tokenJwtService;
    TokenPayloadService tokenPayloadService;
    private final int expiration = 2;

    @BeforeEach
    void setUp() throws Exception {
        HashConfig config = new HashConfig();
        config.setSalt("mon_salt_secret123");
        config.setAlgorithme("HmacSHA256");
        tokenPayloadService = new TokenPayloadService(new HashUtils(config));
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey("mon-secrect-jwt-au-moin-32-chars");
        int secretMinLength = 32;
        jwtProperties.setMinJwtSecretLength(secretMinLength);
        jwtProperties.setExpirationSec(expiration);
        this.tokenJwtService = new TokenJwtService(jwtProperties);
    }

    @Test
    void testHashConsistency() throws Exception {

        TokenPayload payload = tokenPayloadService.createNewUser(new TokenRequest("192.168.1.1"));

        String token = tokenJwtService.generateNewSignedToken(payload);
        Thread.sleep(expiration);
        System.out.println("valeur de token généré: " + token);
        TokenPayload payload1 = tokenJwtService.verifyTokenAndGetPayload(token);

        assertTrue(payload1.getHashedIp().equals(payload.getHashedIp()) && payload1.getUserId().equals(payload.getUserId()),"erreur dans la récupération des données");

        try {
            tokenJwtService.verifyTokenAndGetPayload(token.replace("a","e"));
            throw new RuntimeException("impossible, normalement le token est modifié");//normalement
        }catch (JwtException e)
        {
            System.out.println("Exception de signature valide: " + e.getMessage());
        }
        Thread.sleep(expiration*1000);
        try {
            tokenJwtService.verifyTokenAndGetPayload(token);
            throw new RuntimeException("impossible, normalement le token est éxpiré");//normalement
        }catch (JwtException e)
        {
            System.out.println("Exception temporelle valide: " + e.getMessage());
        }

    }
}
