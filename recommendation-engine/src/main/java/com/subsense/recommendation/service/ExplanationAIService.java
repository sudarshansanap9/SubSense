package com.subsense.recommendation.service;

import com.subsense.recommendation.dto.RecommendationType;
import org.springframework.stereotype.Service;

@Service
public class ExplanationAIService {

    public String generateRecommendationExplanation(String merchant, double avgAmount, int usage, double costPerUse, RecommendationType type) {
        // Mock OpenAI API call
        // In a real scenario, this would format a prompt and call the OpenAI API via RestTemplate or WebClient
        
        switch (type) {
            case CANCEL:
                return String.format("This %s subscription is costing Rs.%.2f/month but was not used recently. Consider canceling it immediately to stop wasting money.", merchant, avgAmount);
            case DOWNGRADE:
                return String.format("You used %s only %d times this period. Your cost per use is extremely high at Rs.%.2f. Consider downgrading to a cheaper plan or paying per use.", merchant, usage, costPerUse);
            case KEEP:
            default:
                return String.format("You are actively using %s %d times per period. The cost per use of Rs.%.2f indicates good value. Keep the subscription.", merchant, usage, costPerUse);
        }
    }
}
