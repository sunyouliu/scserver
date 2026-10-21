package com.example.documentstore.catalog;

import com.example.documentstore.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class DocumentCacheManager {

    private static final Logger log = LoggerFactory.getLogger(DocumentCacheManager.class);

    private final CacheManager cacheManager;

    public DocumentCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void clearDocumentPages() {
        Cache cache = cacheManager.getCache(CacheConfig.DOCUMENT_PAGE_CACHE);
        if (cache != null) {
            cache.clear();
            log.info("已清空文档分页缓存");
        }
    }
}
