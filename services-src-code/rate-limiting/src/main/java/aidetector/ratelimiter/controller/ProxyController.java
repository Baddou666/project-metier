package aidetector.ratelimiter.controller;

import aidetector.ratelimiter.config.GatewayRoutingProperties;
import aidetector.ratelimiter.services.ProxyRoutingService;
import aidetector.ratelimiter.services.RateLimitResponseWriter;
import aidetector.ratelimiter.services.RouteResolutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class ProxyController {

    private final RouteResolutionService routeResolutionService;
    private final ProxyRoutingService proxyRoutingService;
    private final RateLimitResponseWriter rateLimitResponseWriter;

    public ProxyController(RouteResolutionService routeResolutionService,
                           ProxyRoutingService proxyRoutingService, RateLimitResponseWriter rateLimitResponseWriter) {
        this.routeResolutionService = routeResolutionService;
        this.proxyRoutingService = proxyRoutingService;
        this.rateLimitResponseWriter = rateLimitResponseWriter;
    }

    @RequestMapping("/**")
    public void proxy(HttpServletRequest request, HttpServletResponse response)throws Exception {
        try {
            GatewayRoutingProperties.DownstreamRoute route =
                    routeResolutionService.resolveRequiredRoute(request.getRequestURI());
            proxyRoutingService.forward(request, response, route);
        } catch (IllegalArgumentException e) {
            rateLimitResponseWriter.write(response,HttpStatus.NOT_FOUND, Map.of("status","404","message","not found","path",request.getRequestURI()));
        }
    }
}
