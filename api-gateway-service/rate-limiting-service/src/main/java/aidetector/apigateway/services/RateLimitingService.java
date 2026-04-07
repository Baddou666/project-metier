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

    private String redisKey(String userId){
        return "attempts-last-min:" + userId;
    }
    @Override
    public Long addAttempt(String userId){
        String userRedisKey = redisKey(userId);
        Long userAttempts = stringRedisTemplate.opsForValue().increment(userRedisKey);
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(userRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSize()));
        }
        return userAttempts;
    }
    @Override
    public Boolean isRateLimitReached(String userId){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisKey(userId));
        if(stringAttempts != null && !stringAttempts.isBlank())
            return  Long.parseLong(stringAttempts) > rateLimitingConfig.getMaxPerMinAttempts();
        return false;
    }
}
