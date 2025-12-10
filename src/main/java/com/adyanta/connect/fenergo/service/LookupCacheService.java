package com.adyanta.connect.fenergo.service;

import com.adyanta.connect.fenergo.domain.dto.LookupResolution;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching lookup resolutions
 * L1 Cache: Caffeine (in-memory, fast)
 * L2 Cache: Redis (distributed, shared) - TODO: Implement Redis
 */
@Slf4j
@Service
public class LookupCacheService {
    
    private final Cache<String, LookupResolution> l1Cache;
    
    public LookupCacheService() {
        this.l1Cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    }
    
    public LookupResolution get(String key) {
        return l1Cache.getIfPresent(key);
    }
    
    public void put(String key, LookupResolution value, Duration ttl) {
        l1Cache.put(key, value);
        // TODO: Also put in Redis (L2 cache) with TTL
    }
    
    public void evict(String key) {
        l1Cache.invalidate(key);
        // TODO: Also evict from Redis
    }
    
    public void evictAll() {
        l1Cache.invalidateAll();
        // TODO: Also evict all from Redis
    }
}

