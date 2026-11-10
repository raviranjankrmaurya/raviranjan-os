package com.raviranjan.os.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "projects",
                "skills",
                "research",
                "stories",
                "unified_activity",
                "experiences",
                "education",
                "achievements",
                "certifications",
                "lab",
                "articles",
                "profile"
        );
    }
}
