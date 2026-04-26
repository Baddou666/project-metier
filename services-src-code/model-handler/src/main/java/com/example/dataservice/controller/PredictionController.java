package com.example.dataservice.controller;


import com.example.dataservice.requests.PredictionInput;
import com.example.dataservice.responses.PredictionResponse;
import com.example.dataservice.service.JWTService;
import com.example.dataservice.service.PredictionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prediction")
@AllArgsConstructor
public class PredictionController {
    private final PredictionService predictionService;
    private final JWTService jwtService;


    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody PredictionInput predictionInput, @RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        List<PredictionResponse> predictionResponses =
                predictionService.predict(predictionInput.getPredictionRequests(),
                        predictionInput.getLink(), username);
        return ResponseEntity.ok(predictionResponses);
    }
    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(predictionService.getAllUserPredictions(username));
    }


}