package com.example.documentstore.controller;

import com.example.documentstore.payment.PaymentCallbackService;
import com.example.documentstore.payment.WechatCallbackSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-program/virtual-payment/callback")
public class PaymentCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);
    private static final String SUCCESS_XML = "<xml><ErrCode>0</ErrCode><ErrMsg>success</ErrMsg></xml>";
    private static final String FAILURE_XML = "<xml><ErrCode>1</ErrCode><ErrMsg>fail</ErrMsg></xml>";

    private final WechatCallbackSignatureVerifier signatureVerifier;
    private final PaymentCallbackService callbackService;

    public PaymentCallbackController(
            WechatCallbackSignatureVerifier signatureVerifier,
            PaymentCallbackService callbackService
    ) {
        this.signatureVerifier = signatureVerifier;
        this.callbackService = callbackService;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyCallbackUrl(
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String echostr
    ) {
        return signatureVerifier.verify(signature, timestamp, nonce)
                ? ResponseEntity.ok(echostr)
                : ResponseEntity.status(HttpStatus.FORBIDDEN).body("invalid signature");
    }

    @PostMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveCallback(
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestBody String body
    ) {
        if (!signatureVerifier.verify(signature, timestamp, nonce)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(FAILURE_XML);
        }
        try {
            callbackService.handle(body);
            return ResponseEntity.ok(SUCCESS_XML);
        } catch (Exception exception) {
            log.error("处理微信虚拟支付回调失败", exception);
            return ResponseEntity.ok(FAILURE_XML);
        }
    }
}
