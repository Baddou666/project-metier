package aidetector.ratelimiter.exceptions;

public class RateLimitReachedException extends RuntimeException {
    public RateLimitReachedException(String message) {
        super(message);
    }
}
