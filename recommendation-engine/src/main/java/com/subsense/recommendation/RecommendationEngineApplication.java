package com.subsense.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RecommendationEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecommendationEngineApplication.class, args);
    }
}
