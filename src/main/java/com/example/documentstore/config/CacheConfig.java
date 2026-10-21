package com.example.documentstore.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DOCUMENT_PAGE_CACHE = "documentPages";

    @Bean
    public CacheManager cacheManager(AppProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(DOCUMENT_PAGE_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(Math.max(properties.catalog().cacheMaxEntries(), 1))
                .expireAfterWrite(Duration.ofSeconds(Math.max(properties.catalog().cacheTtlSeconds(), 1)))
                .recordStats());
        return cacheManager;
    }
}
