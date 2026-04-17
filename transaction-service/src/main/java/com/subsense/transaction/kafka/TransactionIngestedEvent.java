package com.subsense.transaction.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionIngestedEvent {
    private UUID transactionId;
    private UUID userId;
    private BigDecimal amount;
    private String merchant;
    private String category;
    private Instant timestamp;

    public TransactionIngestedEvent() {}

    public TransactionIngestedEvent(UUID transactionId, UUID userId, BigDecimal amount, String merchant, String category, Instant timestamp) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.merchant = merchant;
        this.category = category;
        this.timestamp = timestamp;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
