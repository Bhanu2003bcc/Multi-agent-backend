package com.research.backend.service;

import com.research.backend.dto.response.ResearchResultResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private static final String RESULT_CACHE_PREFIX = "research:result:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;

    public ResearchResultResponse getCachedResult(String queryHash) {
        try {
            String key = RESULT_CACHE_PREFIX + queryHash;
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof ResearchResultResponse result) {
                log.debug("Redis cache hit for hash={}", queryHash);
                return result;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for hash={}: {}", queryHash, e.getMessage());
        }
        return null;
    }

    public void cacheResult(String queryHash, ResearchResultResponse result) {
        try {
            String key = RESULT_CACHE_PREFIX + queryHash;
            redisTemplate.opsForValue().set(key, result, CACHE_TTL);
            log.debug("Cached research result for hash={}, ttl={}min", queryHash, CACHE_TTL.toMinutes());
        } catch (Exception e) {
            log.warn("Redis cache write failed for hash={}: {}", queryHash, e.getMessage());
            // Non-fatal: pipeline still returns the result
        }
    }

    public void evictResult(String queryHash) {
        try {
            redisTemplate.delete(RESULT_CACHE_PREFIX + queryHash);
        } catch (Exception e) {
            log.warn("Redis eviction failed for hash={}: {}", queryHash, e.getMessage());
        }
    }
}
