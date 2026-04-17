package com.subsense.subscription.repository;

import com.subsense.subscription.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    List<Subscription> findByUserId(UUID userId);
    java.util.Optional<Subscription> findFirstByUserIdAndMerchant(UUID userId, String merchant);
}
