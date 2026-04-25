package aidetector.authentication.services;

public interface TokenGenerationRateLimitManager {
    Boolean isMaxTokenPerIpReached(String srcIp);
    void addTokenToIp(String srcIp);
    Long getIpTokensCount(String ip);
}
