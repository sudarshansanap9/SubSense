package com.subsense.subscription.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_sub_user", columnList = "user_id")
})
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String merchant;

    @Column(nullable = false)
    private double avgAmount;

    @Column(nullable = false)
    private String billingCycle;

    @Column(nullable = false)
    private double confidenceScore;

    @Column(name = "last_detected_at")
    private java.time.Instant lastDetectedAt;

    public Subscription() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public double getAvgAmount() { return avgAmount; }
    public void setAvgAmount(double avgAmount) { this.avgAmount = avgAmount; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public java.time.Instant getLastDetectedAt() { return lastDetectedAt; }
    public void setLastDetectedAt(java.time.Instant lastDetectedAt) { this.lastDetectedAt = lastDetectedAt; }
}
