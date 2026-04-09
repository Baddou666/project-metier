package aidetector.apigateway.model;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;

public class AnonymousIdentification extends AbstractAuthenticationToken {
    private final TokenPayload payload; // On stocke l'objet complet

    public AnonymousIdentification(TokenPayload payload) {
        super(Collections.emptyList());
        this.payload = payload;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // En stateless, on n'a pas besoin de garder le mot de passe
    }

    @Override
    public Object getPrincipal() {
        return this.payload; // C'est ici que l'objet est exposé
    }
}