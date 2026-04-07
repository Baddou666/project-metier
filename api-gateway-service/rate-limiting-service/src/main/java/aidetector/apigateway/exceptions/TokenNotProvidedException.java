package aidetector.apigateway.exceptions;

public class TokenNotProvidedException extends RuntimeException {
    public TokenNotProvidedException(String message) {
        super(message);
    }
}
