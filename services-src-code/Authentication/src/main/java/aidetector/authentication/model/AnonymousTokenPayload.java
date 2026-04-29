package aidetector.authentication.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnonymousTokenPayload {
    private String userId;
    private String userIp;
}
