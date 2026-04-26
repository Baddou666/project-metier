package com.example.dataservice.service;

import com.example.dataservice.entity.Prediction;
import com.example.dataservice.model.ModelClient;
import com.example.dataservice.repository.PredictionRepository;
import com.example.dataservice.requests.PredictionRequest;
import com.example.dataservice.responses.PredictionResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service
public class PredictionService {
    private final ModelClient modelClient;
    private final PredictionRepository predictionRepository;



    public Prediction addPrediction(List<PredictionResponse> predictionResponses, String link, String username){
        double AIMean = 0;
        for (PredictionResponse pr : predictionResponses) {
            if ("LABEL_0".equals(pr.getLabel())) {
                AIMean += pr.getPrediction();
            } else if ("LABEL_1".equals(pr.getLabel())) {
                AIMean += (1.0 - pr.getPrediction());
            }
        }
        AIMean = AIMean / predictionResponses.size();
        Prediction prediction = new Prediction();
        prediction.setAIMeanPrediction(AIMean);
        prediction.setLink(link);
        prediction.setUsername(username);
        return predictionRepository.save(prediction);
    }

    public List<PredictionResponse> predict(List<PredictionRequest> predictionRequests, String link, String username){
        List<PredictionResponse> responses = modelClient.predict(predictionRequests);
        Prediction prediction = this.addPrediction(responses, link, username);
        return responses;
    }
    public List<Prediction> getAllUserPredictions(String username){
        return predictionRepository.findAllByUsername(username);
    }
}
