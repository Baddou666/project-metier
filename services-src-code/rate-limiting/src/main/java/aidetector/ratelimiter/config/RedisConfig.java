package aidetector.ratelimiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
@ConfigurationProperties("api-gateway-service.data.redis")
@Data
public class RedisConfig {

    private String redisHost;
    private int redisPort;
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration();
        redisConf.setHostName(this.redisHost);
        redisConf.setPort(this.redisPort);
        redisConf.setPassword(this.redisPassword);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(200))
                .build();
        return new LettuceConnectionFactory(redisConf, clientConfig);
    }


}
