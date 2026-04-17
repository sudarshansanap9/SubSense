package com.subsense.recommendation.dto;

public class SubscriptionData {
    private String id;
    private String merchant;
    private double avgAmount;
    private String billingCycle;
    private double confidenceScore;
    
    // Mocking usage data for now since we don't have a Usage Engine yet
    private int usageFrequency;

    public SubscriptionData() {}

    public SubscriptionData(String id, String merchant, double avgAmount, String billingCycle, double confidenceScore, int usageFrequency) {
        this.id = id;
        this.merchant = merchant;
        this.avgAmount = avgAmount;
        this.billingCycle = billingCycle;
        this.confidenceScore = confidenceScore;
        this.usageFrequency = usageFrequency;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public double getAvgAmount() { return avgAmount; }
    public void setAvgAmount(double avgAmount) { this.avgAmount = avgAmount; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public int getUsageFrequency() { return usageFrequency; }
    public void setUsageFrequency(int usageFrequency) { this.usageFrequency = usageFrequency; }
}
