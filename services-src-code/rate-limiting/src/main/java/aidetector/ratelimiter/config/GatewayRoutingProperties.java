package aidetector.ratelimiter.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Configuration
@ConfigurationProperties("api-gateway-service.routing")
@Data
public class GatewayRoutingProperties {

    private List<DownstreamRoute> publicRoutes = new ArrayList<>(List.of(
            new DownstreamRoute(
                "token-manager-token-route",
                "/api/anonym-token/get",
                "http://token-manager-service:8080"
            )
    ));

    private List<DownstreamRoute> protectedRoutes = new ArrayList<>(List.of(
            new DownstreamRoute(
                    "ai-detector-route",
                    "/api/ai-detector/**",
                    "http://ai-detector-service:8080"
            )
    ));
    private List<DownstreamRoute> userOnlyRoutes = new ArrayList<>();
    @Data
    public static class DownstreamRoute {
        private String id;
        private String path;
        private String uri;

        public DownstreamRoute() {
        }

        public DownstreamRoute(String id, String path, String uri) {
            this.id = id;
            this.path = path;
            this.uri = uri;
        }
    }

    public Stream<DownstreamRoute> getAllRoutes(){
        return Stream.of(publicRoutes,protectedRoutes,userOnlyRoutes).flatMap(Collection::stream);
    }
}
