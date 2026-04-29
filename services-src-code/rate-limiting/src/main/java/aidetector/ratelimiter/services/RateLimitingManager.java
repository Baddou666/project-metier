package aidetector.ratelimiter.services;

import aidetector.ratelimiter.exceptions.RateLimitReachedException;

public interface RateLimitingManager {
    public Long incrementKeyValue(String key);
    public void verifyAttemptsLimit(String attemptsKey) throws RateLimitReachedException;
    Long getAttempsCountPerToken(String key);
}
