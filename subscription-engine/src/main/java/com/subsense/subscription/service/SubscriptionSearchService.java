package com.subsense.subscription.service;

import com.subsense.subscription.document.SubscriptionDocument;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubscriptionSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SubscriptionSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public List<SubscriptionDocument> searchSubscriptions(
            String userId, String merchant, Double minCost, Double maxCost, String billingCycle, int page, int size) {

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        if (userId != null && !userId.isEmpty()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("userId").value(userId)));
        }

        if (merchant != null && !merchant.isEmpty()) {
            boolQueryBuilder.must(m -> m.match(mq -> mq.field("merchant").query(merchant)));
        }

        if (billingCycle != null && !billingCycle.isEmpty()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("billingCycle").value(billingCycle)));
        }

        if (minCost != null || maxCost != null) {
            boolQueryBuilder.filter(f -> f.range(r -> {
                RangeQuery.Builder range = new RangeQuery.Builder().field("avgAmount");
                if (minCost != null) range.gte(JsonData.of(minCost));
                if (maxCost != null) range.lte(JsonData.of(maxCost));
                return range;
            }));
        }

        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQueryBuilder.build()))
                .withPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "confidenceScore")))
                .build();

        SearchHits<SubscriptionDocument> searchHits = elasticsearchOperations.search(query, SubscriptionDocument.class);

        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}
