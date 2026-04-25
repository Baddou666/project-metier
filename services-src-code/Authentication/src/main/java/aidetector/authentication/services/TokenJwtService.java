package aidetector.authentication.services;

import aidetector.authentication.model.AnonymousTokenPayload;
import aidetector.authentication.utils.CryptoUtils;
import aidetector.authentication.utils.LogContext;
import aidetector.authentication.config.AnonymJwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Service
public class TokenJwtService implements TokenJwtManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenJwtService.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final JWK jwkPublicKeyWithKeyId;
    private final SignatureAlgorithm signAlg;
    private final int expireInSec;

    public TokenJwtService(AnonymJwtProperties jwtProperties, CryptoUtils cryptoUtils) throws Exception{

        JWK jwkPrivateKey = JWK.parseFromPEMEncodedObjects(jwtProperties.getPrivatePemFileContent());
        this.jwkPublicKeyWithKeyId = cryptoUtils.createJwkKeyid(jwkPrivateKey.toPublicJWK());
        this.privateKey = cryptoUtils.extractPrivateKeyFromPem(jwkPrivateKey);
        this.publicKey = cryptoUtils.getPublicKey(jwkPublicKeyWithKeyId);
        this.expireInSec = jwtProperties.getExpirationSec();
        signAlg = cryptoUtils.getSigAlgo(jwkPrivateKey);
        logger.info("[JWT] JWT service initialized with expiration: {} seconds", expireInSec);
    }

    @Override
    public String generateNewSignedToken(AnonymousTokenPayload client){
        LogContext.setEventContext(LogContext.EVENT_TOKEN_GENERATED, null, client.getUserId());
        String token = Jwts.builder()
                .header()
                .keyId(jwkPublicKeyWithKeyId.getKeyID())
                .and()
                .claim("userInfo", client)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (long) expireInSec * 1000))
                .signWith(privateKey,signAlg)
                .compact();
        logger.info("Token generated successfully");
        return token;
    }
    //@Override
    public AnonymousTokenPayload verifyTokenAndGetPayload(String token) throws JwtException, IllegalArgumentException {
        Object payload = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userInfo");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(payload, AnonymousTokenPayload.class);
    }
    @Override
    public JWK getJwkPublicKeyWithId(){
        return this.jwkPublicKeyWithKeyId;
    }
}
