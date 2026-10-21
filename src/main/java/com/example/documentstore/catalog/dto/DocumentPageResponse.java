package com.example.documentstore.catalog.dto;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentResponse> items,
        long total,
        int page,
        int size,
        boolean hasMore
) {
}
