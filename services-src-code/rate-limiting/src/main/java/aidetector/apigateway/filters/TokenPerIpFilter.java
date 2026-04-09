package aidetector.apigateway.filters;

import aidetector.apigateway.config.ProxyHeaderConfig;
import aidetector.apigateway.config.SecurityConfig;
import aidetector.apigateway.model.ApiError;
import aidetector.apigateway.services.RateLimitingManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class TokenPerIpFilter extends OncePerRequestFilter {

    private final RateLimitingManager rateLimitingManager;
    private final ProxyHeaderConfig proxyHeaderConfig;
    public TokenPerIpFilter(RateLimitingManager rateLimitingManager,
                            ProxyHeaderConfig proxyHeaderConfig) {
        this.rateLimitingManager = rateLimitingManager;
        this.proxyHeaderConfig = proxyHeaderConfig;
    }

    @Override
    public void doFilterInternal(HttpServletRequest req, @NonNull HttpServletResponse res,@NonNull FilterChain chain) throws IOException, ServletException {
        if(req.getHeader("Authorization") != null){
            chain.doFilter(req,res);
            return;
        }
        if(!req.getRequestURI().equals(SecurityConfig.newTokenPath)){
            chain.doFilter(req,res);
            return;
        }
        String srcIp = req.getHeader(proxyHeaderConfig.getSourceIpHeader());
        if(srcIp == null || srcIp.isBlank()){
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,"mandatory header not provided");
            return;
        }
        if(!srcIp.matches("^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$")) {
            sendErrorResponse(res, HttpServletResponse.SC_BAD_REQUEST,"header format incorrect");
            return;
        }
        if(rateLimitingManager.isMaxTokenPerIpReached(srcIp)) {
            sendErrorResponse(res, 429, "the maximum token count reached!");
            return;
        }
        chain.doFilter(req,res);
    }


    private void sendErrorResponse(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        ApiError error = new ApiError(status, message, System.currentTimeMillis());
        String json = new ObjectMapper().writeValueAsString(error);
        res.getWriter().write(json);
    }

}
