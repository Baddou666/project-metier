package aidetector.ratelimiter.services;

import aidetector.ratelimiter.config.GatewayRoutingProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Service
public class RouteResolutionService {

    private final GatewayRoutingProperties gatewayRoutingProperties;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public RouteResolutionService(GatewayRoutingProperties gatewayRoutingProperties) {
        this.gatewayRoutingProperties = gatewayRoutingProperties;
    }

    public boolean isPublicRoute(String requestPath) {
        return gatewayRoutingProperties.getPublicRoutes()
                .stream()
                .anyMatch(route -> antPathMatcher.match(route.getPath(), requestPath));
    }

    public boolean isProtectedRoute(String requestPath) {
        return gatewayRoutingProperties.getProtectedRoutes()
                .stream()
                .anyMatch(route -> antPathMatcher.match(route.getPath(), requestPath));
    }

    public GatewayRoutingProperties.DownstreamRoute resolveRequiredRoute(String requestPath) {
        return gatewayRoutingProperties.getAllRoutes()
                .filter(route -> antPathMatcher.match(route.getPath(), requestPath))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No downstream route configured for " + requestPath));
    }
}
