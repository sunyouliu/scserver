package com.example.documentstore.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentOrderRequest(
        @NotBlank String productId
) {
}
