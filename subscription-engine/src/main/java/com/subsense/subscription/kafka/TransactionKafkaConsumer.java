package com.subsense.subscription.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subsense.subscription.dto.TransactionIngestedEvent;
import com.subsense.subscription.service.IdempotencyService;
import com.subsense.subscription.service.SubscriptionDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TransactionKafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionKafkaConsumer.class);
    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final SubscriptionDetectionService detectionService;

    public TransactionKafkaConsumer(ObjectMapper objectMapper, IdempotencyService idempotencyService, SubscriptionDetectionService detectionService) {
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.detectionService = detectionService;
    }

    @KafkaListener(topics = "subsense.transactions.ingest", groupId = "subscription-engine-group")
    public void consume(String message, Acknowledgment ack) {
        try {
            logger.info("Raw event consumed from Kafka: {}", message);
            TransactionIngestedEvent event = objectMapper.readValue(message, TransactionIngestedEvent.class);

            // Constraint: Idempotency deduplication
            if (!idempotencyService.isUniqueEventAndSet(event.getTransactionId().toString())) {
                ack.acknowledge();
                return;
            }

            // Route to Detection Algorithm
            detectionService.processEvent(event);

            ack.acknowledge(); // Manual ACK to prevent data loss on crash
            logger.info("Successfully processed and ACKed event for transactionId: {}", event.getTransactionId());

        } catch (Exception e) {
            logger.error("Error processing Kafka message. Message pushed back for retry. Error: {}", e.getMessage(), e);
            // Throw exception to trigger Spring Kafka retry / dead-letter strategy mechanism
            throw new RuntimeException("Error processing Kafka message", e);
        }
    }
}
