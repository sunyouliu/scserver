package com.example.documentstore.download.dto;

public record FreeDownloadQuotaResponse(
        String date,
        int limit,
        int used,
        int remaining
) {
}
