package aidetector.ratelimiter.filters;
/*
import aidetector.ratelimiter.config.ProxyHeaderConfig;
import aidetector.ratelimiter.model.ApiError;
import aidetector.ratelimiter.services.RateLimitingManager;
import aidetector.ratelimiter.utils.LogContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class TokenPerIpFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TokenPerIpFilter.class);

    private final RateLimitingManager rateLimitingManager;
    private final ProxyHeaderConfig proxyHeaderConfig;
    public TokenPerIpFilter(RateLimitingManager rateLimitingManager,
                            ProxyHeaderConfig proxyHeaderConfig) {
        this.rateLimitingManager = rateLimitingManager;
        this.proxyHeaderConfig = proxyHeaderConfig;
    }

    @Override
    public void doFilterInternal(HttpServletRequest req, @NonNull HttpServletResponse res,@NonNull FilterChain chain) throws IOException, ServletException {
        String uri = req.getRequestURI();
        String authHeader = req.getHeader("Authorization");
        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, null);
        LogContext.addDetail(LogContext.MESSAGE_DETAIL, uri);

        if(req.getHeader("Authorization") != null){
            logger.debug("Authorization header present, skipping IP filter");
            LogContext.clear();
            chain.doFilter(req,res);
            return;
        }
        if(!req.getRequestURI().equals(SecurityConfig.newTokenPath)){
            logger.debug("Not token path, skipping");
            LogContext.clear();
            chain.doFilter(req,res);
            return;
        }
        String srcIp = req.getHeader(proxyHeaderConfig.getSourceIpHeader());
        LogContext.setEventContext(LogContext.EVENT_IP_VALIDATION, srcIp);

        if(srcIp == null || srcIp.isBlank()){
            LogContext.addDetail(LogContext.STATUS, "MISSING_IP_HEADER");
            logger.warn("Mandatory header missing");
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,"mandatory header not provided");
            return;
        }
        if(!srcIp.matches("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$")) {
            LogContext.addDetail(LogContext.STATUS, "INVALID_IP_FORMAT");
            logger.warn("Invalid IP format");
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,"header format incorrect");
            return;
        }
        if(rateLimitingManager.isMaxTokenPerIpReached(srcIp)) {
            LogContext.addDetail(LogContext.STATUS, "RATE_LIMIT_REACHED");
            logger.warn("Token limit reached");
            sendErrorResponse(res, 429, "the maximum token count reached!");
            return;
        }
        LogContext.addDetail(LogContext.STATUS, "PASSED");
        logger.info("IP check passed");
        LogContext.clear();
        chain.doFilter(req,res);
    }


    private void sendErrorResponse(HttpServletResponse res, int status, String message) throws IOException {
        LogContext.addDetail(LogContext.MESSAGE_DETAIL, message);
        logger.error("Sending error response");
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(status, message, System.currentTimeMillis());
        String json = new ObjectMapper().writeValueAsString(error);
        res.getWriter().write(json);
    }

}
*/