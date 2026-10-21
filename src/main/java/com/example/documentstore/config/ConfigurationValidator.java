package com.example.documentstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(OrderedConfiguration.FIRST)
public class ConfigurationValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationValidator.class);
    private static final int MIN_JWT_SECRET_LENGTH = 32;
    private static final int MIN_SESSION_KEY_SECRET_LENGTH = 16;

    private final AppProperties properties;

    public ConfigurationValidator(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        List<String> missing = new ArrayList<>();
        require(missing, "WECHAT_APP_ID", properties.wechat().appId());
        require(missing, "WECHAT_APP_SECRET", properties.wechat().appSecret());
        require(missing, "JWT_SECRET", properties.security().jwtSecret());
        require(missing, "SESSION_KEY_ENCRYPTION_SECRET", properties.security().sessionKeyEncryptionSecret());

        if (!isBlank(properties.security().jwtSecret())
                && properties.security().jwtSecret().length() < MIN_JWT_SECRET_LENGTH) {
            missing.add(String.format("JWT_SECRET（至少 %d 个字符）", MIN_JWT_SECRET_LENGTH));
        }
        if (!isBlank(properties.security().sessionKeyEncryptionSecret())
                && properties.security().sessionKeyEncryptionSecret().length() < MIN_SESSION_KEY_SECRET_LENGTH) {
            missing.add(String.format("SESSION_KEY_ENCRYPTION_SECRET（至少 %d 个字符）", MIN_SESSION_KEY_SECRET_LENGTH));
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("缺少或无效的服务端环境变量：" + String.join("、", missing));
        }

        if (isBlank(properties.virtualPayment().offerId()) || isBlank(properties.virtualPayment().appKey())) {
            log.warn("虚拟支付 OfferID 或 AppKey 未配置，下单接口将返回 503");
        }
        if (isBlank(properties.virtualPayment().callbackToken())) {
            log.warn("WECHAT_CALLBACK_TOKEN 未配置，支付回调将被拒绝");
        }
    }

    private void require(List<String> missing, String name, String value) {
        if (isBlank(value)) {
            missing.add(name);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
