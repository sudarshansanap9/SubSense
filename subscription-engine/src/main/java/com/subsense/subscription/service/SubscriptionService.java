package com.subsense.subscription.service;

import com.subsense.subscription.document.SubscriptionDocument;
import com.subsense.subscription.entity.Subscription;
import com.subsense.subscription.repository.SubscriptionRepository;
import com.subsense.subscription.repository.SubscriptionSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSearchRepository searchRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, SubscriptionSearchRepository searchRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.searchRepository = searchRepository;
    }

    @Transactional
    public Subscription createSubscription(Subscription subscription) {
        log.info("Saving subscription for user {} and merchant {}", subscription.getUserId(), subscription.getMerchant());
        Subscription saved = subscriptionRepository.save(subscription);
        
        // Sync to Elasticsearch
        syncToElasticsearch(saved);
        
        return saved;
    }

    private void syncToElasticsearch(Subscription subscription) {
        try {
            SubscriptionDocument doc = new SubscriptionDocument();
            doc.setId(subscription.getId().toString());
            doc.setUserId(subscription.getUserId().toString());
            doc.setMerchant(subscription.getMerchant());
            doc.setAvgAmount(subscription.getAvgAmount());
            doc.setBillingCycle(subscription.getBillingCycle());
            doc.setConfidenceScore(subscription.getConfidenceScore());
            
            searchRepository.save(doc);
            log.debug("Successfully synced subscription {} to Elasticsearch", doc.getId());
        } catch (Exception e) {
            log.error("Failed to sync subscription {} to Elasticsearch", subscription.getId(), e);
            // Consider sending to a reliable DLQ or retry mechanism in production
        }
    }
}
