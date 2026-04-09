package aidetector.apigateway;

import aidetector.apigateway.config.HashConfig;
import aidetector.apigateway.config.RateLimitingConfig;
import aidetector.apigateway.config.RedisConfig;
import aidetector.apigateway.model.TokenPayload;
import aidetector.apigateway.model.TokenRequest;
import aidetector.apigateway.services.RateLimitingManager;
import aidetector.apigateway.services.RateLimitingService;
import aidetector.apigateway.services.TokenPayloadService;
import aidetector.apigateway.utils.HashUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingTest {

    TokenPayloadService tokenPayloadService;
    private  StringRedisTemplate stringRedisTemplate;
    private RateLimitingConfig rateLimitingConfig;
    private RedisConfig redisConfig;
    LettuceConnectionFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        HashConfig config = new HashConfig();
        config.setSalt("mon_salt_secret123");
        config.setAlgorithme("HmacSHA256");
        tokenPayloadService = new TokenPayloadService(new HashUtils(config));
        this.rateLimitingConfig = new RateLimitingConfig();
        this.rateLimitingConfig.setMaxAttemptsPerToken(500L);
        this.rateLimitingConfig.setTokenGenerationDelayMs(500L);
        this.rateLimitingConfig.setWindowSizePerToken(10L);
        this.redisConfig = new RedisConfig();


        // 2. Injecter les valeurs à la main (ce que Spring fait d'habitude)
        redisConfig.setRedisHost("localhost");
        redisConfig.setRedisPort(6379);
        redisConfig.setRedisPassword("votre_mot_de_passe_robuste");

        factory = redisConfig.redisConnectionFactory();

        // 4. Initialiser la factory (Étape CRUCIALE que Spring fait normalement)
        factory.afterPropertiesSet();
        factory.start();

        this.stringRedisTemplate = new StringRedisTemplate(factory);
    }

    @Test
    void testRedisConnectionManually() {

        // 4. Initialiser la factory (Étape CRUCIALE que Spring fait normalement)
        factory.afterPropertiesSet();
        factory.start();

        try {
            // Vérifier si la connexion est fonctionnelle
            factory.getConnection().ping();
            System.out.println("Connexion réussie !");
        } finally {
            factory.destroy();
        }
    }

    @Test
    void testHashConsistency() throws Exception {

        TokenPayload payload = tokenPayloadService.createNewUser(new TokenRequest("192.168.1.1"));

        RateLimitingManager test = new RateLimitingService(this.stringRedisTemplate,this.rateLimitingConfig);

        test.addAttempt(payload.getUserId());
        // test si la vérification est correcte
        assertFalse(test.isRateLimitReached(payload.getUserId()), "erreur normalement la limite n'est pas encore atteinte !" );
        while(test.getUserAttempts(payload.getUserId()) <= rateLimitingConfig.getMaxAttemptsPerToken()) {
            test.addAttempt(payload.getUserId());
        }
        assertTrue(test.isRateLimitReached(payload.getUserId()), "erreur normalement la limite est atteinte!");

        // test si le rafraichissement fonctionne
        TokenPayload payload1 = tokenPayloadService.createNewUser(new TokenRequest("192.168.1.1"));
        test.addAttempt(payload1.getUserId());
        assertFalse(test.isRateLimitReached(payload1.getUserId()), "erreur normalement la limite n'est pas encore atteinte !" );
        LocalTime ref = LocalTime.now();
        long requestsPeriod = 1000L;
        while(!test.isRateLimitReached(payload1.getUserId())){
            test.addAttempt(payload1.getUserId());
            Long t = ChronoUnit.SECONDS.between(ref,LocalTime.now());
            System.out.printf("attempts: (%d / %d) :: (%d/%d)\n\n\n",
                    test.getUserAttempts(payload1.getUserId()),
                    rateLimitingConfig.getMaxAttemptsPerToken(),
                    t,
                    rateLimitingConfig.getWindowSizePerToken());
            if(t >= rateLimitingConfig.getWindowSizePerToken()) {
                ref = LocalTime.now();
                requestsPeriod /= 10;
            }
            Thread.sleep(requestsPeriod);
        }

    }

}
