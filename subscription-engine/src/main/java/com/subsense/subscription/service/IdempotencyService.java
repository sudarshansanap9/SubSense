package com.subsense.subscription.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a transactionId has already been processed using Redis SETNX.
     * 
     * @param transactionId The unique event ID
     * @return true if the event has NEVER been processed (safe to proceed); false if it is a duplicate
     */
    public boolean isUniqueEventAndSet(String transactionId) {
        String key = PROCESSED_EVENT_PREFIX + transactionId;
        // TTL 30 days to avoid duplicate replays but keep Redis slim
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofDays(30));
        
        if (Boolean.FALSE.equals(success)) {
            logger.warn("Idempotency Collision: Event {} already processed. Skipping.", transactionId);
            return false;
        }
        return true;
    }
}
