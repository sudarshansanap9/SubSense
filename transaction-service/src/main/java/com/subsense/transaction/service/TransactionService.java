package com.subsense.transaction.service;

import com.subsense.transaction.dto.TransactionRequest;
import com.subsense.transaction.dto.TransactionResponse;
import com.subsense.transaction.entity.Transaction;
import com.subsense.transaction.kafka.TransactionEventPublisher;
import com.subsense.transaction.kafka.TransactionIngestedEvent;
import com.subsense.transaction.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository transactionRepository;
    private final TransactionEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository, TransactionEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        logger.info("Received transaction ingestion request for user: {}", request.getUserId());

        Transaction transaction = new Transaction();
        transaction.setUserId(request.getUserId());
        transaction.setAmount(request.getAmount());
        transaction.setMerchant(request.getMerchant());
        transaction.setCategory(request.getCategory());
        transaction.setTimestamp(request.getTimestamp());

        try {
            transaction = transactionRepository.saveAndFlush(transaction);
            logger.info("Transaction safely logged in Database with ID: {}", transaction.getId());
        } catch (DataIntegrityViolationException e) {
            logger.warn("Idempotent block: Duplicate transaction detected for user {} at {}, dropping duplicate.", request.getUserId(), request.getTimestamp());
            // Requirement 8: If DB fails -> DO NOT publish event
            return new TransactionResponse(null, "DUPLICATE_IGNORED");
        }

        // 4. Publish to Kafka STRICTLY AFTER DB commit
        TransactionIngestedEvent event = new TransactionIngestedEvent(
                transaction.getId(),
                transaction.getUserId(),
                transaction.getAmount(),
                transaction.getMerchant(),
                transaction.getCategory(),
                transaction.getTimestamp()
        );
        eventPublisher.publish(event);

        return new TransactionResponse(transaction.getId(), "SUCCESS");
    }
}
