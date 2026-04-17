package com.subsense.subscription.repository;

import com.subsense.subscription.document.SubscriptionDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionSearchRepository extends ElasticsearchRepository<SubscriptionDocument, String> {
    List<SubscriptionDocument> findByUserId(String userId);
}
