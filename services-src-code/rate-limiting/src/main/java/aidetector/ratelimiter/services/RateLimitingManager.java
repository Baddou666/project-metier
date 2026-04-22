package aidetector.ratelimiter.services;

import aidetector.ratelimiter.exceptions.RateLimitReachedException;

public interface RateLimitingManager {
    public void incrementKeyValue(String key);
    public void verifyAttemptsLimit(String attemptsKey) throws RateLimitReachedException;
    public Long getKeyValue(String key);
    public void addTokenToIp(String srcIp);
    public Boolean isMaxTokenPerIpReached(String srcIp);
    public Long getIpTokensCount(String ip);
}
