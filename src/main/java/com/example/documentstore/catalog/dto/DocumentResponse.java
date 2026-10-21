package com.example.documentstore.catalog.dto;

public record DocumentResponse(
        String id,
        String title,
        int imageCount,
        int priceFen,
        String thumbnailUrl,
        String detailImageUrl,
        boolean freeLimited
) {
}
