package aidetector.ratelimiter.controller;

import aidetector.ratelimiter.config.RateLimitingConfig;
import aidetector.ratelimiter.exceptions.InvalidIpException;
import aidetector.ratelimiter.exceptions.InvalidTokenException;
import aidetector.ratelimiter.exceptions.InvalidTokenSignatureException;
import aidetector.ratelimiter.exceptions.RateLimitReachedException;
import aidetector.ratelimiter.model.IpCheckRequest;
import aidetector.ratelimiter.model.RateLimiterResponse;
import aidetector.ratelimiter.model.TokenAttemptsRequest;
import aidetector.ratelimiter.services.RateLimitingManager;
import aidetector.ratelimiter.utils.LogContext;
import io.jsonwebtoken.lang.Strings;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;

@RestController("/api/rate-limiter")
public class RateLimitingController {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingController.class);
    private final RateLimitingManager rateLimitingManager;
    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitingController(RateLimitingManager rateLimitingManager,
                                  RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingManager = rateLimitingManager;
        this.rateLimitingConfig = rateLimitingConfig;
    }


    @GetMapping("/verify-ip-max-tokens")
    public ResponseEntity<?> verifyIpTokensNumber(@RequestBody IpCheckRequest ip){

        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION,ip.getIpv4(),null);
        try {
            InetAddress ipObj = InetAddress.getByName(ip.getIpv4());
            ip.setIpv4(ipObj.getHostAddress());
            if (ip.getIpv4() == null) throw new InvalidIpException("provided null ip");
            logger.info("ip verification succeeded");
        }
        catch(Exception e) {
            logger.warn(e.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_BAD_REQUEST).body(new RateLimiterResponse(null,"ip format not valid or null"));
        }
        finally {
            LogContext.clear();
        }
        if(rateLimitingManager.isMaxTokenPerIpReached(ip.getIpv4()))
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).body(new RateLimiterResponse(true,"max allowed token reached !"));
        return ResponseEntity.accepted().body(new RateLimiterResponse(false,"%s has %d/%d, resets every %d seconds".formatted(ip.getIpv4(),rateLimitingManager.getIpTokensCount(ip.getIpv4()),rateLimitingConfig.getMaxTokenPerIp(),rateLimitingConfig.getWindowSizePerIp())));
    }


    @GetMapping("/verify-token-attempts")
    public ResponseEntity<?> verifyTokenAttempts(@RequestBody TokenAttemptsRequest attemptsRequest){



        LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK,attemptsRequest.getUserIp(),attemptsRequest.getUserId());
        if(attemptsRequest.getToken()==null || !Strings.hasText(attemptsRequest.getToken())) {
            logger.debug("No token present");
            LogContext.clear();
            return ResponseEntity.badRequest().body(new RateLimiterResponse(null,"No token present"));
        }


        try {
            verifyTokenSignature(attemptsRequest.getToken());
            LogContext.clear();
            LogContext.setEventContext(LogContext.EVENT_RATE_LIMIT_CHECK, attemptsRequest.getUserIp(), attemptsRequest.getUserId());
            String tokenKey = hashToken(attemptsRequest.getToken());
            rateLimitingManager.verifyAttemptsLimit(tokenKey);
            LogContext.clear();
            return ResponseEntity.accepted().body(
                    new RateLimiterResponse(false,
                            "%s(attempts: %d/%d, resets every %d seconds)".formatted(
                                    attemptsRequest.getUserId(),
                                    rateLimitingManager.getKeyValue(tokenKey),
                                    rateLimitingConfig.getMaxAttemptsPerToken(),
                                    rateLimitingConfig.getWindowSizePerToken())));


        }
        catch (RateLimitReachedException e)
        {
            return ResponseEntity.badRequest().body(new RateLimiterResponse(null, e.getMessage()));
        }catch (InvalidTokenException e)
        {
            LogContext.clear();
            LogContext.setEventContext(LogContext.EVENT_TOKEN_CHECK, attemptsRequest.getUserIp(),attemptsRequest.getUserId());
            LogContext.addDetail(LogContext.TOKEN_SIGNATURE,"no match");
            logger.warn("token validation failed");
            LogContext.clear();
            return ResponseEntity.badRequest().body(new RateLimiterResponse(null, e.getMessage()));
        }

    }
    @PostMapping("/add-verify-token-attempt")
    public ResponseEntity<?> addTokenAttempt(@RequestBody TokenAttemptsRequest attemptsRequest){
        LogContext.setEventContext(LogContext.ATTEMPTS,attemptsRequest.getUserIp(),attemptsRequest.getUserId());

        if(attemptsRequest.getToken() != null){
            rateLimitingManager.incrementKeyValue(hashToken(attemptsRequest.getToken()));
            logger.info("incremented token attempts");
            LogContext.clear();
        }
        return verifyTokenAttempts(attemptsRequest);
    }
    @PostMapping("/add-verify-ip-token")
    public ResponseEntity<?> addIpToken(@RequestBody IpCheckRequest ipObj){
        ResponseEntity<?> res = verifyIpTokensNumber(ipObj);
        if (res.getStatusCode().is2xxSuccessful())
            rateLimitingManager.addTokenToIp(ipObj.getIpv4());
        return res;
    }

    private void verifyTokenSignature(String token) {
        throw new InvalidTokenSignatureException("i did not implement the token verification yet");
    }

    private String hashToken(String token)
    {
        return DigestUtils.sha256Hex(token);
    }
}
