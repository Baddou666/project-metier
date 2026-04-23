package aidetector.authentication.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthApiResponse {
    private int status;
    private String message;
    private String token;
}