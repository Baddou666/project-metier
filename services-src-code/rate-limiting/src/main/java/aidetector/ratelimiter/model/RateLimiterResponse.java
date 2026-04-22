package aidetector.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RateLimiterResponse {
    private Boolean isLimiteReached;
    private String message;
}
