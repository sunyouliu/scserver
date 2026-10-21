package com.example.documentstore.controller;

import com.example.documentstore.payment.VirtualPaymentService;
import com.example.documentstore.payment.dto.CreatePaymentOrderRequest;
import com.example.documentstore.payment.dto.DownloadLinkResponse;
import com.example.documentstore.payment.dto.PaymentOrderResponse;
import com.example.documentstore.security.AuthInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-program/virtual-payment/orders")
public class VirtualPaymentController {

    private final VirtualPaymentService paymentService;

    public VirtualPaymentController(VirtualPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentOrderResponse createOrder(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) {
        return paymentService.createOrder(userId, request);
    }

    @GetMapping("/{outTradeNo}/download-link")
    public ResponseEntity<DownloadLinkResponse> getDownloadLink(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable String outTradeNo
    ) {
        DownloadLinkResponse response = paymentService.getDownloadLink(userId, outTradeNo);
        return "PAID".equals(response.status())
                ? ResponseEntity.ok(response)
                : ResponseEntity.accepted().body(response);
    }
}
