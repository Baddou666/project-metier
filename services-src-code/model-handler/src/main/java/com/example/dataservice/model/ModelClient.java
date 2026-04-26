package com.example.dataservice.model;

import com.example.dataservice.requests.PredictionRequest;
import com.example.dataservice.responses.PredictionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class ModelClient {

    private final WebClient webClient;

    public ModelClient(@Value("${model.api.url}") String modelApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(modelApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<PredictionResponse> predict(List<PredictionRequest> items) {

        return webClient.post()
                .uri("/predict")
                .bodyValue(items) // ✅ send the list directly
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody -> {
                                    System.out.println(">>> Python error: " + errorBody);
                                    return new RuntimeException(errorBody);
                                })
                )
                .bodyToMono(new ParameterizedTypeReference<List<PredictionResponse>>() {})
                .block();
    }
}
