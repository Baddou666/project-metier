package aidetector.ratelimiter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class ReactiveResponseWriter {

    private final ObjectMapper objectMapper;

    public ReactiveResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, Object body) {
        byte[] payload = serialize(body);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(payload);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private byte[] serialize(Object body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            return "{\"status\":\"500\",\"message\":\"response serialization failed\"}"
                    .getBytes(StandardCharsets.UTF_8);
        }
    }
}
