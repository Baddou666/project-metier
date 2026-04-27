package com.example.dataservice.controller;


import com.example.dataservice.requests.PredictionInput;
import com.example.dataservice.responses.ModelPredictionResponse;
import com.example.dataservice.responses.PredictionResponse;
import com.example.dataservice.service.JWTService;
import com.example.dataservice.service.PredictionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-detector")

public class PredictionController {
    private final PredictionService predictionService;
    private final JWTService jwtService;
    private final String isAnonymHeaderName;
    public PredictionController(JWTService jwtService,
                                PredictionService predictionService,
                                @Value("${ai-detector.network.is-anonymous-user-header}")
                                String isAnonymHeaderName){
        this.predictionService = predictionService;
        this.jwtService = jwtService;
        this.isAnonymHeaderName=isAnonymHeaderName;
    }


    @PostMapping("/detect")
    public ResponseEntity<?> predict(@RequestBody PredictionInput predictionInput,
                                     HttpServletRequest req){
        List<PredictionResponse> predictionResponses;
        if(!Boolean.parseBoolean(req.getHeader(isAnonymHeaderName))){
            String authHeader = req.getHeader("Authorization");
            if(authHeader == null)
                return ResponseEntity.badRequest().body(Map.of("status","error","service","model-handler","message","Auth header is mandatory for connected users"));
            if(!authHeader.startsWith("Bearer "))
                return ResponseEntity.badRequest().body(Map.of("status","error","service","model-handler","message","Auth header schema must be Bearer"));

            String token = authHeader.substring(7);
            String username = jwtService.extractUsername(token);
            predictionResponses = predictionService.predictForConnectedUser(predictionInput.getItems(),
                            predictionInput.getLink(), username);
        }
        else {
            predictionResponses = predictionService.predictForAnonymUser(predictionInput.getItems());
        }
        return ResponseEntity.ok(predictionResponses);
    }
    @GetMapping("/history")
    public ResponseEntity<?> getUserHistory(HttpServletRequest req){
        if(Boolean.getBoolean(req.getHeader(isAnonymHeaderName))){
            return ResponseEntity.badRequest().body(Map.of("status","dumb","message","a non connected user does not have an history"));
        }
        String authHeader = req.getHeader("Authorization");
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        return ResponseEntity.ok(predictionService.getAllUserPredictions(username));
    }


}