package com.techno.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Simple in-memory cache using ConcurrentHashMap
     * Suitable for single-instance deployments
     *
     * For production/clustered environments, consider:
     * - Redis (Spring Data Redis)
     * - Hazelcast
     * - Caffeine with distributed sync
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("systemConfig");
    }
}
