package com.example.documentstore.payment;

import com.example.documentstore.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;

@Component
public class WechatCallbackSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WechatCallbackSignatureVerifier.class);
    private static final Duration MAX_TIMESTAMP_AGE = Duration.ofMinutes(5);

    private final AppProperties properties;

    public WechatCallbackSignatureVerifier(AppProperties properties) {
        this.properties = properties;
    }

    public boolean verify(String signature, String timestamp, String nonce) {
        if (isBlank(signature) || isBlank(timestamp) || isBlank(nonce)) {
            return false;
        }

        long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException exception) {
            return false;
        }
        Instant requestTime = Instant.ofEpochSecond(epochSeconds);
        Instant now = Instant.now();
        if (requestTime.isAfter(now.plus(MAX_TIMESTAMP_AGE)) || requestTime.isBefore(now.minus(MAX_TIMESTAMP_AGE))) {
            return false;
        }

        String callbackToken = properties.virtualPayment().callbackToken();
        if (isBlank(callbackToken)) {
            log.warn("未配置 WECHAT_CALLBACK_TOKEN，拒绝支付回调");
            return false;
        }

        String[] values = {callbackToken, timestamp, nonce};
        Arrays.sort(values);
        try {
            byte[] expected = HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-1").digest(String.join("", values).getBytes(StandardCharsets.UTF_8)))
                    .getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("微信回调签名校验失败", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
