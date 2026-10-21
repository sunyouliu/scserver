package com.example.documentstore.payment.dto;

public record DownloadLinkResponse(
        String status,
        String outTradeNo,
        String downloadLink
) {
}
