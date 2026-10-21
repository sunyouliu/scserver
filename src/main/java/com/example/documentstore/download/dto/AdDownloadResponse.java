package com.example.documentstore.download.dto;

public record AdDownloadResponse(
        String status,
        String documentId,
        String downloadLink
) {
}
