package aidetector.ratelimiter.model;

import lombok.Data;

@Data
public class TokenAttemptsRequest {
    private String token;
    private String userId;
    private String userIp;
}
