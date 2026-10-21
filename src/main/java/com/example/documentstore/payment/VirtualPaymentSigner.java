package com.example.documentstore.payment;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class VirtualPaymentSigner {

    public String createPaySignature(String appKey, String signData) {
        return hmacSha256(appKey, "requestVirtualPayment&" + signData);
    }

    public String createUserSignature(String sessionKey, String signData) {
        return hmacSha256(sessionKey, signData);
    }

    private String hmacSha256(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("虚拟支付签名生成失败", exception);
        }
    }
}
