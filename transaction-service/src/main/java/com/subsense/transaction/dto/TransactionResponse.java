package com.subsense.transaction.dto;

import java.util.UUID;

public class TransactionResponse {
    
    private UUID transactionId;
    private String status;

    public TransactionResponse() {}

    public TransactionResponse(UUID transactionId, String status) {
        this.transactionId = transactionId;
        this.status = status;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
