package aidetector.apigateway.exceptions;

public class TokenPayloadVerificationFailed extends RuntimeException {
    public TokenPayloadVerificationFailed(String message) {
        super(message);
    }
}
