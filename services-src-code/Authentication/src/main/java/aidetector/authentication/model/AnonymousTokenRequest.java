package aidetector.authentication.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnonymousTokenRequest {
    private String srcIp;
}
