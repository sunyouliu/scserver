package com.example.documentstore.payment.dto;

import java.util.Map;

public record PaymentOrderResponse(
        String status,
        String outTradeNo,
        Map<String, Object> paymentParams,
        String downloadLink
) {
}
