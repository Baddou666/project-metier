package aidetector.authentication.exceptions;

public class TokenGenerationFailedException extends RuntimeException {
    public TokenGenerationFailedException(String message) {
        super(message);
    }
}
