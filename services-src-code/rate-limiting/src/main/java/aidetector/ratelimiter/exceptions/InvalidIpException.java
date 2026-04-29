package aidetector.ratelimiter.exceptions;

public class InvalidIpException extends RuntimeException {
    public InvalidIpException(String message) {
        super(message);
    }
}
