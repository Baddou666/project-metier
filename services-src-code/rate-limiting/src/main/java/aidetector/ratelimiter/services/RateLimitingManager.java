package aidetector.ratelimiter.services;

import aidetector.ratelimiter.exceptions.RateLimitReachedException;

public interface RateLimitingManager {
    public Long incrementKeyValue(String key);
    public void verifyAttemptsLimit(String attemptsKey) throws RateLimitReachedException;
    public void addTokenToIp(String srcIp);
    Long getAttempsCountPerToken(String key);
    public Boolean isMaxTokenPerIpReached(String srcIp);
    public Long getIpTokensCount(String ip);
}
