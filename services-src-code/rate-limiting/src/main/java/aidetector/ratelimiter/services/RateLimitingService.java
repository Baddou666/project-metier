package aidetector.ratelimiter.services;

import aidetector.ratelimiter.config.RateLimitingConfig;
import aidetector.ratelimiter.exceptions.RateLimitReachedException;
import aidetector.ratelimiter.utils.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitingService implements RateLimitingManager {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitingService(StringRedisTemplate stringRedisTemplate, RateLimitingConfig rateLimitingConfig) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.rateLimitingConfig = rateLimitingConfig;
    }

    private String redisAttemptsKey(String attemptsKey){
        return "attempts:" + attemptsKey;
    }
    @Override
    public Long incrementKeyValue(String attemptsKey){
        String userRedisKey = redisAttemptsKey(attemptsKey);
        Long userAttempts = stringRedisTemplate.opsForValue().increment(userRedisKey);
        LogContext.setEventContext(LogContext.EVENT_ATTEMPTS_COUNT_MODIFIED, null, attemptsKey);
        LogContext.addDetail(LogContext.REDIS_KEY, userRedisKey);
        LogContext.addDetail(LogContext.COUNTER_VALUE, userAttempts);
        logger.debug("Attempt added to counter");
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(userRedisKey,
                        Duration.ofSeconds(rateLimitingConfig.getWindowSizePerToken()));
        }
        return userAttempts;
    }
    @Override
    public void verifyAttemptsLimit(String attemptsKey) throws RateLimitReachedException{
        Long attempts = getLiterralKeyValue(redisAttemptsKey(attemptsKey));
        boolean reached = attempts > rateLimitingConfig.getMaxAttemptsPerToken();
        LogContext.addDetail(LogContext.ATTEMPTS, attempts);
        LogContext.addDetail(LogContext.RATE_LIMIT, rateLimitingConfig.getMaxAttemptsPerToken());
        LogContext.addDetail(LogContext.LIMIT_REACHED, reached);
        LogContext.addDetail(LogContext.STATUS, reached ? "RATE_LIMIT_REACHED" : "ALLOWED");
        if (reached) {
            logger.warn("Rate limit reached for user");
            throw new RateLimitReachedException("Rate limit reached for user");
        } else {
            logger.debug("Rate limit check passed");
        }
    }
    @Override
    public Long getAttempsCountPerToken(String key){
        return getLiterralKeyValue(redisAttemptsKey(key));
    }

    private Long getLiterralKeyValue(String attemptsKey){
        String stringAttempts = stringRedisTemplate.opsForValue().get(attemptsKey);
        long attempts = 0L;
        if(stringAttempts != null && !stringAttempts.isBlank())
            attempts = Long.parseLong(stringAttempts);
        return attempts;
    }
}
