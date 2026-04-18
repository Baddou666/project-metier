package aidetector.apigateway.services;

import aidetector.apigateway.config.RateLimitingConfig;
import aidetector.apigateway.utils.LogContext;
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
        LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, null, userId);
        LogContext.addDetail(LogContext.REDIS_KEY, userRedisKey);
        LogContext.addDetail(LogContext.COUNTER_VALUE, userAttempts);
        logger.debug("Attempt added to counter");
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(userRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSizePerToken()));
        }
    }
    @Override
    public Boolean isRateLimitReached(String userId){
        Long attempts = getUserAttempts(userId);
        Boolean reached = attempts > rateLimitingConfig.getMaxAttemptsPerToken();
        LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, null, userId);
        LogContext.addDetail(LogContext.ATTEMPTS, attempts);
        LogContext.addDetail(LogContext.RATE_LIMIT, rateLimitingConfig.getMaxAttemptsPerToken());
        LogContext.addDetail(LogContext.LIMIT_REACHED, reached);
        if (reached) {
            logger.warn("Rate limit reached for user");
        } else {
            logger.debug("Rate limit check passed");
        }
        return reached;
    }
    @Override
    public Long getUserAttempts(String userId){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisAttemptsKey(userId));
        Long attempts = 0L;
        if(stringAttempts != null && !stringAttempts.isBlank())
            attempts = Long.parseLong(stringAttempts);
        return attempts;
    }
    @Override
    public void addToken(String srcIp){
        String tokenRedisKey = redisTokensKey(srcIp);
        Long userAttempts = stringRedisTemplate.opsForValue().increment(tokenRedisKey);
        LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, srcIp, null);
        LogContext.addDetail(LogContext.REDIS_KEY, tokenRedisKey);
        LogContext.addDetail(LogContext.COUNTER_VALUE, userAttempts);
        logger.debug("Token counter incremented");
        if(userAttempts!= null){
            if(userAttempts == 1)
                stringRedisTemplate.expire(tokenRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSizePerIp()));
        }
    }

    @Override
    public Boolean isMaxTokenPerIpReached(String srcIp){
        Long attempts = getTokenPerIp(srcIp);
        Boolean reached = attempts >= rateLimitingConfig.getMaxTokenPerIp();
        LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, srcIp, null);
        LogContext.addDetail(LogContext.TOKEN_COUNT, attempts);
        LogContext.addDetail(LogContext.RATE_LIMIT, rateLimitingConfig.getMaxTokenPerIp());
        LogContext.addDetail(LogContext.LIMIT_REACHED, reached);
        if (reached) {
            logger.warn("Token limit reached for IP");
        } else {
            logger.debug("Token limit check passed");
        }
        return reached;
    }

    @Override
    public Long getTokenPerIp(String srcIp){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisTokensKey(srcIp));
        Long attempts = 0L;
        if(stringAttempts != null && !stringAttempts.isBlank())
            attempts = Long.parseLong(stringAttempts);
        return attempts;
    }
}
