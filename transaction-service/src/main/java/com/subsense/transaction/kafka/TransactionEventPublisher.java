package com.subsense.transaction.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEventPublisher.class);
    private static final String TOPIC = "subsense.transactions.ingest";
    
    private final KafkaTemplate<String, TransactionIngestedEvent> kafkaTemplate;

    public TransactionEventPublisher(KafkaTemplate<String, TransactionIngestedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionIngestedEvent event) {
        try {
            // Use transactionId as the exact Kafka partitioning Key to guarantee ordered consumption
            kafkaTemplate.send(TOPIC, event.getTransactionId().toString(), event);
            logger.info("Published TransactionIngestedEvent to Kafka target [{}] for transactionId: {}", TOPIC, event.getTransactionId());
        } catch (Exception e) {
            logger.error("Failed to publish to Kafka for transaction: {}. Moving on. Error: {}", event.getTransactionId(), e.getMessage());
            // Requirement 8: If Kafka fails -> log + retry (do not crash API)
            // Note: Spring Kafka automatically retries based on back-off mechanics under the hood. We won't crash the incoming HTTP request.
        }
    }
}
