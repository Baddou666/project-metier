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
    private String redisTokensKey(String srcIp){
        return "token-count:" + srcIp;
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
    @Override
    public void addTokenToIp(String srcIp){
        String tokenRedisKey = redisTokensKey(srcIp);
        Long ipAttempts = stringRedisTemplate.opsForValue().increment(tokenRedisKey);
        LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, srcIp, null);
        LogContext.addDetail(LogContext.REDIS_KEY, tokenRedisKey);
        LogContext.addDetail(LogContext.COUNTER_VALUE, ipAttempts);
        logger.debug("token count incremented");
        if(ipAttempts!= null){
            if(ipAttempts == 1)
                stringRedisTemplate.expire(tokenRedisKey, Duration.ofSeconds(rateLimitingConfig.getWindowSizePerIp()));
        }
    }

    @Override
    public Boolean isMaxTokenPerIpReached(String srcIp){
        Long attempts = getLiterralKeyValue(redisTokensKey(srcIp));
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
    public Long getIpTokensCount(String ip){
        String stringAttempts = stringRedisTemplate.opsForValue().get(redisTokensKey(ip));
        long attempts = 0L;
        if(stringAttempts != null && !stringAttempts.isBlank())
            attempts = Long.parseLong(stringAttempts);
        return attempts;
    }


}
