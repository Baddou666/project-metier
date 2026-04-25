package aidetector.authentication.services;

import aidetector.authentication.config.TokenGenerationRateLimitConfig;
import aidetector.authentication.utils.LogContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenGenerationRateLimitService implements TokenGenerationRateLimitManager {

    private static final Logger logger = LoggerFactory.getLogger(TokenGenerationRateLimitService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenGenerationRateLimitConfig tokenGenerationRateLimitConfig;

    public TokenGenerationRateLimitService(StringRedisTemplate stringRedisTemplate,
                                           TokenGenerationRateLimitConfig tokenGenerationRateLimitConfig) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.tokenGenerationRateLimitConfig = tokenGenerationRateLimitConfig;
    }

    private String redisTokensKey(String srcIp) {
        return "token-count:" + srcIp;
    }

    @Override
    public void addTokenToIp(String srcIp) {
        String tokenRedisKey = redisTokensKey(srcIp);
        Long ipAttempts = stringRedisTemplate.opsForValue().increment(tokenRedisKey);
        LogContext.setEventContext(LogContext.EVENT_TOKEN_COUNT_MODIFIED, srcIp, null);
        LogContext.addDetail(LogContext.REDIS_KEY, tokenRedisKey);
        LogContext.addDetail(LogContext.COUNTER_VALUE, ipAttempts);
        logger.debug("token count incremented");
        if (ipAttempts != null && ipAttempts == 1) {
            stringRedisTemplate.expire(
                    tokenRedisKey,
                    Duration.ofSeconds(tokenGenerationRateLimitConfig.getWindowSizePerIp())
            );
        }
    }

    @Override
    public Boolean isMaxTokenPerIpReached(String srcIp) {
        Long attempts = getLiteralKeyValue(redisTokensKey(srcIp));
        Boolean reached = attempts >= tokenGenerationRateLimitConfig.getMaxTokenPerIp();
        LogContext.setEventContext(LogContext.EVENT_TOKEN_GENERATION_PERMISSION, srcIp, null);
        LogContext.addDetail(LogContext.TOKEN_COUNT, attempts);
        LogContext.addDetail(LogContext.RATE_LIMIT, tokenGenerationRateLimitConfig.getMaxTokenPerIp());
        LogContext.addDetail(LogContext.LIMIT_REACHED, reached);
        LogContext.addDetail(LogContext.STATUS, reached ? "RATE_LIMIT_REACHED" : "ALLOWED");
        if (reached) {
            logger.warn("Token limit reached for IP");
        } else {
            logger.debug("Token limit check passed");
        }
        return reached;
    }

    @Override
    public Long getIpTokensCount(String ip) {
        return getLiteralKeyValue(redisTokensKey(ip));
    }

    private Long getLiteralKeyValue(String attemptsKey) {
        String stringAttempts = stringRedisTemplate.opsForValue().get(attemptsKey);
        long attempts = 0L;
        if (stringAttempts != null && !stringAttempts.isBlank()) {
            attempts = Long.parseLong(stringAttempts);
        }
        return attempts;
    }
}
