package com.example.documentstore.catalog;

import com.example.documentstore.catalog.dto.DocumentPageResponse;
import com.example.documentstore.catalog.dto.DocumentResponse;
import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.example.documentstore.config.CacheConfig;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.repository.DocumentAssetRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
public class DocumentCatalogService {

    private final AppProperties properties;
    private final DocumentAssetRepository repository;
    private final DocumentImageResolver imageResolver;

    public DocumentCatalogService(
            AppProperties properties,
            DocumentAssetRepository repository,
            DocumentImageResolver imageResolver
    ) {
        this.properties = properties;
        this.repository = repository;
        this.imageResolver = imageResolver;
    }

    @Cacheable(cacheNames = CacheConfig.DOCUMENT_PAGE_CACHE, key = "#keyword + ':' + #pageNumber + ':' + #pageSize")
    @Transactional(readOnly = true)
    public DocumentPageResponse findPage(String keyword, int pageNumber, int pageSize) {
        Page<DocumentAsset> page = repository.search(
                normalizeKeyword(keyword),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "sortOrder"))
        );
        return new DocumentPageResponse(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public DocumentResponse findById(String id) {
        DocumentAsset asset = repository.findById(id).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "文档不存在"));
        return toResponse(asset);
    }

    private String normalizeKeyword(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private DocumentResponse toResponse(DocumentAsset asset) {
        String imageUrl = buildImageUrl(asset.getId());
        return new DocumentResponse(
                asset.getId(),
                asset.getTitle(),
                asset.getImageCount(),
                resolvePriceFen(asset),
                imageUrl,
                imageUrl,
                properties.freeDownload().enabled()
        );
    }

    private int resolvePriceFen(DocumentAsset asset) {
        Integer priceFen = asset.getPriceFen();
        return priceFen != null && priceFen > 0 ? priceFen : properties.virtualPayment().priceFen();
    }

    private String buildImageUrl(String id) {
        String urlPrefix = normalizeUrlPrefix(properties.catalog().imageUrlPrefix());
        if (urlPrefix.isBlank()) {
            return "";
        }
        String fileName = imageResolver.resolveFileName(id);
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        return urlPrefix + "/" + UriUtils.encodePathSegment(fileName, StandardCharsets.UTF_8);
    }

    private String normalizeUrlPrefix(String urlPrefix) {
        if (urlPrefix == null || urlPrefix.isBlank()) {
            return "";
        }
        String normalized = urlPrefix.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/+$", "");
    }
}
