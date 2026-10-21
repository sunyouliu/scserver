package com.example.documentstore.payment;

public record PaymentNotification(
        String event,
        String outTradeNo,
        String openId,
        String platformOrderNo,
        Integer amountFen
) {
}
