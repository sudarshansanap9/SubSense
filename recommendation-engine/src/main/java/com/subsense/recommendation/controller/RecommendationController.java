package com.subsense.recommendation.controller;

import com.subsense.recommendation.dto.RecommendationResponse;
import com.subsense.recommendation.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(@PathVariable String userId) {
        List<RecommendationResponse> responses = recommendationService.generateRecommendations(userId);
        return ResponseEntity.ok(responses);
    }
}
