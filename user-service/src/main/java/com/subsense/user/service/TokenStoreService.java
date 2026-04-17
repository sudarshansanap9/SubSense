package com.subsense.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TokenStoreService {

    private static final Logger log = LoggerFactory.getLogger(TokenStoreService.class);
    private final StringRedisTemplate redisTemplate;

    public TokenStoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationMs) {
        redisTemplate.opsForValue().set("blacklist:" + token, "revoked", Duration.ofMillis(expirationMs));
        log.info("Token blacklisted in Redis.");
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    public void storeRefreshToken(String userId, String refreshToken, long expirationMs) {
        redisTemplate.opsForValue().set("refresh:" + userId, refreshToken, Duration.ofMillis(expirationMs));
    }

    public boolean validateRefreshToken(String userId, String providedToken) {
        String storedToken = redisTemplate.opsForValue().get("refresh:" + userId);
        return storedToken != null && storedToken.equals(providedToken);
    }
    
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("refresh:" + userId);
    }
}
