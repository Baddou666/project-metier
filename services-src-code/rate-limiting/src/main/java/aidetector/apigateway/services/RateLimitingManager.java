package aidetector.apigateway.services;

public interface RateLimitingManager {
    public void addAttempt(String userId);
    public Boolean isRateLimitReached(String userId);
    public Long getUserAttempts(String userId);
    public void addToken(String srcIp);
    public Boolean isMaxTokenPerIpReached(String srcIp);
    public Long getTokenPerIp(String srcIp);
}
