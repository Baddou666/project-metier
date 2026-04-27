package com.example.dataservice.model;

import com.example.dataservice.requests.ModelPredictionRequest;
import com.example.dataservice.responses.ModelPredictionResponse;
import com.example.dataservice.responses.PredictionResponse;
import com.example.dataservice.service.PredictionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class ModelClient {

    private final WebClient webClient;

    public ModelClient(@Value("${ai-detector.detector-api.url}") String modelApiUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(modelApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<ModelPredictionResponse> predict(List<ModelPredictionRequest> items) {

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
                .bodyToMono(new ParameterizedTypeReference<List<ModelPredictionResponse>>() {})
                .block();
    }
}
