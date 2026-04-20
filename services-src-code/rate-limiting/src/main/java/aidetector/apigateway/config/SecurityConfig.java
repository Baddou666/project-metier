package aidetector.apigateway.config;

import aidetector.apigateway.filters.TokenPerIpFilter;
import aidetector.apigateway.filters.TokenRateLimitePerUserFilter;
import aidetector.apigateway.services.RateLimitingManager;
import aidetector.apigateway.services.TokenJwtManager;
import aidetector.apigateway.services.TokenPayloadManager;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {
    public static final String newTokenPath="/api/token/get";
    public static final String healthPath="/api/health";
    public static final String verifyTokenPath="/api/token/verify";
    private static final String[] AUTH_WHITELIST = {
            newTokenPath,
            healthPath
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenRateLimitePerUserFilter tokenRateLimitePerUserFilter,
                                                   TokenPerIpFilter tokenPerIpFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .anyRequest().authenticated()
        )
                .addFilterBefore(tokenRateLimitePerUserFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tokenPerIpFilter,TokenRateLimitePerUserFilter.class);


        return http.build();
    }
}