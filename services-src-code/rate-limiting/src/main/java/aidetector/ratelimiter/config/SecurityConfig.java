package aidetector.ratelimiter.config;

import aidetector.ratelimiter.filters.ProtectedTokenRateLimitFilter;
import aidetector.ratelimiter.filters.TokenCreationRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           TokenCreationRateLimitFilter tokenCreationRateLimitFilter,
                                           ProtectedTokenRateLimitFilter protectedTokenRateLimitFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(tokenCreationRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(protectedTokenRateLimitFilter, TokenCreationRateLimitFilter.class);
        return http.build();
    }
}
