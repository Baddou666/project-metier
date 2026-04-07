package aidetector.apigateway.exceptions;

public class RateLimitReachedException extends RuntimeException {
    public RateLimitReachedException(String message) {
        super(message);
    }
}
