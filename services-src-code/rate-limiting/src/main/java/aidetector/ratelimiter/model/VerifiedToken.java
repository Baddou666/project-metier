package aidetector.ratelimiter.model;

public record VerifiedToken(
        String tokenHash,
        String userId,
        String userIp
) {
}
