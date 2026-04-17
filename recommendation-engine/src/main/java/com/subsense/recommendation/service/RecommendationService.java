package com.subsense.recommendation.service;

import com.subsense.recommendation.dto.RecommendationResponse;
import com.subsense.recommendation.dto.RecommendationType;
import com.subsense.recommendation.dto.SubscriptionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final ExplanationAIService explanationAIService;

    @Value("${app.recommendation.thresholds.downgrade-cost-per-use}")
    private double downgradeCostThreshold;

    public RecommendationService(ExplanationAIService explanationAIService) {
        this.explanationAIService = explanationAIService;
    }

    public RecommendationResponse analyzeSubscription(SubscriptionData data) {
        log.info("Analyzing subscription for merchant: {}", data.getMerchant());
        
        double costPerUse = 0;
        RecommendationType type;

        if (data.getUsageFrequency() <= 0) {
            costPerUse = data.getAvgAmount(); // Waste calculation
            type = RecommendationType.CANCEL;
        } else {
            costPerUse = data.getAvgAmount() / data.getUsageFrequency();
            if (costPerUse > downgradeCostThreshold) {
                type = RecommendationType.DOWNGRADE;
            } else {
                type = RecommendationType.KEEP;
            }
        }

        String explanation = explanationAIService.generateRecommendationExplanation(
                data.getMerchant(), data.getAvgAmount(), data.getUsageFrequency(), costPerUse, type);

        return new RecommendationResponse(data.getMerchant(), costPerUse, type, explanation);
    }

    @org.springframework.cache.annotation.Cacheable(value = "userRecommendations", key = "#userId")
    public List<RecommendationResponse> generateRecommendations(String userId) {
        // Mocking the call to the subscription-engine that would return a list of SubscriptionData
        // In a real microservice architecture, we'd use RestTemplate, WebClient, or FeignClient to fetch this.
        log.info("Fetching raw subscriptions to generate recommendations for user: {}", userId);
        
        List<SubscriptionData> mockSubscriptions = List.of(
                new SubscriptionData("sub-1", "Netflix", 649.0, "MONTHLY", 95.0, 1),
                new SubscriptionData("sub-2", "Gym", 1500.0, "MONTHLY", 90.0, 0),
                new SubscriptionData("sub-3", "Spotify", 119.0, "MONTHLY", 99.0, 45)
        );

        List<RecommendationResponse> responses = new ArrayList<>();
        for (SubscriptionData sub : mockSubscriptions) {
            responses.add(analyzeSubscription(sub));
        }

        return responses;
    }
}
