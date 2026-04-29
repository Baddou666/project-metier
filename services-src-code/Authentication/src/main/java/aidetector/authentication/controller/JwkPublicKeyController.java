package aidetector.authentication.controller;

import aidetector.authentication.services.TokenJwtManager;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/.well-known/jwks.json")
public class JwkPublicKeyController {

    private final TokenJwtManager tokenJwtManager;

    public JwkPublicKeyController(TokenJwtManager tokenJwtManager) {
        this.tokenJwtManager = tokenJwtManager;
    }

    @GetMapping
    public Map<String, Object> getJwks() {
        return new JWKSet(tokenJwtManager.getJwkPublicKeyWithId()).toJSONObject();
    }
}
