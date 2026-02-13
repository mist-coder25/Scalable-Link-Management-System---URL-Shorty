package com.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    // Fallback in-memory storage
    private final java.util.concurrent.ConcurrentHashMap<String, Long> requestCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> guestRequestCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> expiryMap = new java.util.concurrent.ConcurrentHashMap<>();

    public RateLimiterService(@org.springframework.beans.factory.annotation.Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MS = 60000;

    public boolean isAllowed(String clientIp) {
        if (redisTemplate != null) {
            try {
                String key = "rate_limit:" + clientIp;
                Long requests = redisTemplate.opsForValue().increment(key);
                if (requests != null && requests == 1) {
                    redisTemplate.expire(key, java.util.Objects.requireNonNull(java.time.Duration.ofMinutes(1)));
                }
                return requests != null && requests <= MAX_REQUESTS_PER_MINUTE;
            } catch (Exception e) {
                // Redis failed, fall through to memory
            }
        }
        
        // In-memory fallback
        long now = System.currentTimeMillis();
        
        Long expiry = expiryMap.get(clientIp);
        if (expiry == null || now > expiry) {
            requestCounts.put(clientIp, 0L);
            expiryMap.put(clientIp, now + WINDOW_MS);
        }
        
        return requestCounts.merge(clientIp, 1L, (a, b) -> a + b) <= MAX_REQUESTS_PER_MINUTE;
    }

    private static final int GUEST_LIMIT_PER_HOUR = 5;
    private static final long GUEST_WINDOW_MS = 3600000;

    public boolean isGuestAllowed(String clientIp) {
        if (redisTemplate != null) {
            try {
                String key = "rate_limit_guest:" + clientIp;
                Long requests = redisTemplate.opsForValue().increment(key);
                if (requests != null && requests == 1) {
                    redisTemplate.expire(key, java.util.Objects.requireNonNull(java.time.Duration.ofHours(1)));
                }
                return requests != null && requests <= GUEST_LIMIT_PER_HOUR;
            } catch (Exception e) {
                // Fallback
            }
        }
        
        // Simple guest fallback
        String key = "guest:" + clientIp;
        long now = System.currentTimeMillis();
        
        Long expiry = expiryMap.get(key);
        if (expiry == null || now > expiry) {
             guestRequestCounts.put(key, 0L);
             expiryMap.put(key, now + GUEST_WINDOW_MS);
        }
        
        return guestRequestCounts.merge(key, 1L, (a, b) -> a + b) <= GUEST_LIMIT_PER_HOUR;
    }
}
