package aidetector.apigateway.services;

public interface RateLimitingManager {
    public Long addAttempt(String userId);
    public Boolean isRateLimitReached(String userId);
}
