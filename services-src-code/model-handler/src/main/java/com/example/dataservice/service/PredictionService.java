package com.example.dataservice.service;

import com.example.dataservice.entity.Prediction;
import com.example.dataservice.model.ModelClient;
import com.example.dataservice.repository.PredictionRepository;
import com.example.dataservice.requests.ModelPredictionRequest;
import com.example.dataservice.responses.ModelPredictionResponse;
import com.example.dataservice.responses.PredictionResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
public class PredictionService {
    private final ModelClient modelClient;
    private final PredictionRepository predictionRepository;
    public static final String humainLabel = "LABEL_1";
    public static final String aiLabel = "LABEL_0";


    public Prediction addPrediction(List<ModelPredictionResponse> predictionResponses, String link, String username){
        double AIMean = 0;
        for (ModelPredictionResponse pr : predictionResponses) {
            if (aiLabel.equals(pr.getLabel())) {
                AIMean += pr.getPrediction();
            } else if (humainLabel.equals(pr.getLabel())) {
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

    public List<PredictionResponse> predictForConnectedUser(List<ModelPredictionRequest> predictionRequests, String link, String username){
        List<ModelPredictionResponse> responses = modelClient.predict(predictionRequests);
        Prediction prediction = this.addPrediction(responses, link, username);
        return formatPredicitonResult(responses);
    }

    public List<PredictionResponse> predictForAnonymUser(List<ModelPredictionRequest> predictionRequests){
        return formatPredicitonResult(modelClient.predict(predictionRequests));

    }
    public List<Prediction> getAllUserPredictions(String username){
        return predictionRepository.findAllByUsername(username);
    }

    private List<PredictionResponse> formatPredicitonResult(List<ModelPredictionResponse> modelResponses){
        List<PredictionResponse> predictResponses = new ArrayList<>();
        for(ModelPredictionResponse modelRes : modelResponses){
            predictResponses.add(new PredictionResponse(modelRes.getId(),
                    modelRes.getLabel().equals(PredictionService.aiLabel),
                    modelRes.getLabel().equals(PredictionService.aiLabel) ? modelRes.getPrediction():(1 - modelRes.getPrediction())));
        }
        return predictResponses;
    }
}
