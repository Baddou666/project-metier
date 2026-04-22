package aidetector.authentication.services;

import aidetector.authentication.model.TokenPayload;
import aidetector.authentication.utils.LogContext;
import aidetector.authentication.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenJwtService implements TokenJwtManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenJwtService.class);

    private final SecretKey jwtSecretKey;
    private final int expireInSec;

    public TokenJwtService(JwtProperties jwtProperties){
        String tempsec = jwtProperties.getSecretKey();
        if(tempsec == null || tempsec.isBlank() || tempsec.strip().length() < jwtProperties.getMinJwtSecretLength()){
            throw new IllegalArgumentException("the jwt secret should have at least %d characters long !".formatted(jwtProperties.getMinJwtSecretLength()));
        }
        this.jwtSecretKey = Keys.hmacShaKeyFor(tempsec.getBytes(StandardCharsets.UTF_8));
        this.expireInSec = jwtProperties.getExpirationSec();
        logger.info("[JWT] JWT service initialized with expiration: {} seconds", expireInSec);
    }

    @Override
    public String generateNewSignedToken(TokenPayload client){
        LogContext.setEventContext(LogContext.EVENT_TOKEN_GENERATED, null, client.getUserId());
        String token = Jwts.builder()
                .header().add("typ", "JWT")
                .and()
                .subject("rate-limite")
                .claim("userInfo", client)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (long) expireInSec * 1000))
                .signWith(jwtSecretKey)
                .compact();
        logger.info("Token generated successfully");
        return token;
    }
    @Override
    public TokenPayload verifyTokenAndGetPayload(String token) throws JwtException, IllegalArgumentException {
        LogContext.setEventContext(LogContext.EVENT_TOKEN_VERIFIED, null, null);
        Object payload = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userInfo");
        ObjectMapper mapper = new ObjectMapper();
        TokenPayload tokenPayload = mapper.convertValue(payload,TokenPayload.class);
        LogContext.addDetail(LogContext.USER_ID, tokenPayload.getUserId());
        logger.info("Token verified successfully");
        return tokenPayload;
    }
}
