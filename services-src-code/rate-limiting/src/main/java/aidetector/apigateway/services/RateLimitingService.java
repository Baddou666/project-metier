package aidetector.apigateway.services;

import aidetector.apigateway.config.RateLimitingConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitingService implements RateLimitingManager {


    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitingService(StringRedisTemplate stringRedisTemplate, RateLimitingConfig rateLimitingConfig) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitingConfig = rateLimitingConfig;
    }

    private String redisAttemptsKey(String userId){
        return "attempts:" + userId;
    }
    private String redisTokensKey(String srcIp){
        return "token-count:" + srcIp;
    }
    @Override
    public void addAttempt(String userId){
        String userRedisKey = redisAttemptsKey(userId);
        Long userAttempts = stringRedisTemplate.opsForValue().increment(userRedisKey);
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(userRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSizePerToken()));
        }
    }
    @Override
    public Boolean isRateLimitReached(String userId){
        Long attempts = getUserAttempts(userId);
        return  attempts > rateLimitingConfig.getMaxAttemptsPerToken();
    }
    @Override
    public Long getUserAttempts(String userId){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisAttemptsKey(userId));
        if(stringAttempts != null && !stringAttempts.isBlank())
            return  Long.parseLong(stringAttempts);
        return 0L;
    }
    @Override
    public void addToken(String srcIp){
        String tokenRedisKey = redisTokensKey(srcIp);
        Long userAttempts = stringRedisTemplate.opsForValue().increment(tokenRedisKey);
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(tokenRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSizePerIp()));
        }
    }

    @Override
    public Boolean isMaxTokenPerIpReached(String srcIp){
        Long attempts = getTokenPerIp(srcIp);
        return  attempts >= rateLimitingConfig.getMaxTokenPerIp();
    }

    @Override
    public Long getTokenPerIp(String srcIp){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisTokensKey(srcIp));
        if(stringAttempts != null && !stringAttempts.isBlank())
            return  Long.parseLong(stringAttempts);
        return 0L;
    }
}
