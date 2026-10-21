package com.example.documentstore.download.dto;

public record FreeDownloadResponse(
        String status,
        String documentId,
        String downloadLink
) {
}
