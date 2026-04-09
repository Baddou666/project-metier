package aidetector.apigateway.services;

import aidetector.apigateway.config.JwtProperties;
import aidetector.apigateway.model.TokenPayload;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenJwtService implements TokenJwtManager {
    private final SecretKey jwtSecretKey;
    private final int expireInSec;

    public TokenJwtService(JwtProperties jwtProperties){
        String tempsec = jwtProperties.getSecretKey();
        if(tempsec == null || tempsec.isBlank() || tempsec.strip().length() < jwtProperties.getMinJwtSecretLength()){
            throw new IllegalArgumentException("the jwt secret should have at least %d characters long !".formatted(jwtProperties.getMinJwtSecretLength()));
        }
        this.jwtSecretKey = Keys.hmacShaKeyFor(tempsec.getBytes(StandardCharsets.UTF_8));
        this.expireInSec = jwtProperties.getExpirationSec();
    }

    //crée un token signé qui contient le hash de l'adresse ip d'un utilisateur
    @Override
    public String generateNewSignedToken(TokenPayload client){

        return Jwts.builder()
                .header().add("typ", "JWT")
                .and()
                .subject("rate-limite")
                .claim("userInfo", client)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + (long) expireInSec * 1000))
                .signWith(jwtSecretKey)
                .compact();
    }
    @Override
    public TokenPayload verifyTokenAndGetPayload(String token) throws JwtException, IllegalArgumentException {

        Object payload = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userInfo");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(payload,TokenPayload.class);
    }



}
