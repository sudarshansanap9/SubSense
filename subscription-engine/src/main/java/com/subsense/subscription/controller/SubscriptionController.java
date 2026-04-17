package com.subsense.subscription.controller;

import com.subsense.subscription.document.SubscriptionDocument;
import com.subsense.subscription.service.SubscriptionSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@org.springframework.web.bind.annotation.CrossOrigin(origins = "*")
public class SubscriptionController {

    private final SubscriptionSearchService searchService;

    public SubscriptionController(SubscriptionSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SubscriptionDocument>> search(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String merchant,
            @RequestParam(required = false) Double minCost,
            @RequestParam(required = false) Double maxCost,
            @RequestParam(required = false) String billingCycle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
            
        long startTime = System.currentTimeMillis();
        List<SubscriptionDocument> results = searchService.searchSubscriptions(userId, merchant, minCost, maxCost, billingCycle, page, size);
        long duration = System.currentTimeMillis() - startTime;
        
        // Add custom header to show < 150ms achievement
        return ResponseEntity.ok()
                .header("X-Response-Time-ms", String.valueOf(duration))
                .body(results);
    }
}
