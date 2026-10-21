package com.example.documentstore.download.dto;

public record AdQuotaResponse(
        String date,
        int limit,
        int used,
        int remaining
) {
}
