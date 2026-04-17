package com.subsense.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subsense.subscription.document.SubscriptionDocument;
import com.subsense.subscription.dto.TransactionIngestedEvent;
import com.subsense.subscription.entity.Subscription;
import com.subsense.subscription.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionDetectionService.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SubscriptionRepository subscriptionRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public SubscriptionDetectionService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                                        SubscriptionRepository subscriptionRepository,
                                        ElasticsearchOperations elasticsearchOperations) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.subscriptionRepository = subscriptionRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public void processEvent(TransactionIngestedEvent event) throws JsonProcessingException {
        String cacheKey = "tx_cache:" + event.getUserId() + ":" + event.getMerchant();

        // 1. Fetch historical from Redis (Optional caching mechanism per spec)
        List<TransactionIngestedEvent> history = new ArrayList<>();
        String historyJson = redisTemplate.opsForValue().get(cacheKey);
        if (historyJson != null) {
            history = objectMapper.readValue(historyJson, new TypeReference<List<TransactionIngestedEvent>>() {});
        }

        // Add new and sort
        history.add(event);
        history.sort(Comparator.comparing(TransactionIngestedEvent::getTimestamp));

        // Limit size to prevent bloat (keep last 12 transactions for optimization)
        if (history.size() > 12) {
            history = history.subList(history.size() - 12, history.size());
        }

        // Save back to Redis cache
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(history), Duration.ofDays(60));

        // 3. Core Engine: Subscription Detection Logic
        if (history.size() < 2) {
            logger.debug("Not enough data to detect subscription for {}", event.getMerchant());
            return;
        }

        double totalIntervalDays = 0;
        int validIntervals = 0;
        BigDecimal totalAmount = history.get(0).getAmount();
        
        for (int i = 1; i < history.size(); i++) {
            Instant prev = history.get(i - 1).getTimestamp();
            Instant curr = history.get(i).getTimestamp();
            double days = Duration.between(prev, curr).toDays();
            if (days >= 1) {
                totalIntervalDays += days;
                validIntervals++;
            }
            totalAmount = totalAmount.add(history.get(i).getAmount());
        }

        if (validIntervals == 0) {
            logger.debug("Only same-day transactions detected for {}, skipping interval match", event.getMerchant());
            return;
        }

        double avgInterval = totalIntervalDays / validIntervals;
        BigDecimal avgAmount = totalAmount.divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        // Calculate amount variance (must be <= 10%)
        boolean amountConsistent = true;
        for (TransactionIngestedEvent tx : history) {
            BigDecimal diff = tx.getAmount().subtract(avgAmount).abs();
            if (diff.divide(avgAmount, 4, RoundingMode.HALF_UP).compareTo(new BigDecimal("0.10")) > 0) {
                amountConsistent = false;
                break;
            }
        }

        if (!amountConsistent) {
            return; // Irregular spending, likely not a subscription
        }

        // Rule evaluation based on time
        String billingCycle = "UNKNOWN";
        double confidenceScore = 0;

        if (Math.abs(avgInterval - 30) <= 5) {
            billingCycle = "MONTHLY";
            confidenceScore = 80 + Math.min(20, history.size() * 5.0); // Up to 100
        } else if (Math.abs(avgInterval - 7) <= 2) {
            billingCycle = "WEEKLY";
            confidenceScore = 85 + Math.min(15, history.size() * 4.0);
        } else if (Math.abs(avgInterval - 365) <= 15) {
            billingCycle = "YEARLY";
            confidenceScore = 90;
        }

        if (!"UNKNOWN".equals(billingCycle)) {
            saveAndSyncSubscription(event, avgAmount, billingCycle, Math.min(100, confidenceScore));
        }
    }

    private void saveAndSyncSubscription(TransactionIngestedEvent event, BigDecimal avgAmount, String billingCycle, double confidenceScore) {
        // Find existing or create new
        Optional<Subscription> existingOpt = subscriptionRepository.findFirstByUserIdAndMerchant(event.getUserId(), event.getMerchant());
        Subscription subscription = existingOpt.orElse(new Subscription());
        
        subscription.setUserId(event.getUserId());
        subscription.setMerchant(event.getMerchant());
        subscription.setAvgAmount(avgAmount.doubleValue());
        subscription.setBillingCycle(billingCycle);
        subscription.setConfidenceScore(confidenceScore);
        subscription.setLastDetectedAt(event.getTimestamp());

        // Save DB
        subscription = subscriptionRepository.save(subscription);
        logger.info("Subscription logically detected and SAVED: {} (Score: {})", subscription.getMerchant(), confidenceScore);

        // Sync ES natively (Constraint 6: ES writes only AFTER DB persistence)
        SubscriptionDocument doc = new SubscriptionDocument();
        doc.setId(subscription.getId().toString());
        doc.setUserId(subscription.getUserId().toString());
        doc.setMerchant(subscription.getMerchant());
        doc.setAvgAmount(subscription.getAvgAmount());
        doc.setBillingCycle(subscription.getBillingCycle());
        doc.setConfidenceScore(subscription.getConfidenceScore());

        elasticsearchOperations.save(doc);
        logger.info("Subscription securely indexed inside Elasticsearch layer: {}", doc.getId());
    }
}
