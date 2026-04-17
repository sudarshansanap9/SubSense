package com.subsense.recommendation.dto;

import java.io.Serializable;

public class RecommendationResponse implements Serializable {
    private String merchant;
    private double costPerUse;
    private RecommendationType recommendationType;
    private String explanation;

    public RecommendationResponse() {}

    public RecommendationResponse(String merchant, double costPerUse, RecommendationType recommendationType, String explanation) {
        this.merchant = merchant;
        this.costPerUse = costPerUse;
        this.recommendationType = recommendationType;
        this.explanation = explanation;
    }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public double getCostPerUse() { return costPerUse; }
    public void setCostPerUse(double costPerUse) { this.costPerUse = costPerUse; }
    public RecommendationType getRecommendationType() { return recommendationType; }
    public void setRecommendationType(RecommendationType recommendationType) { this.recommendationType = recommendationType; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
