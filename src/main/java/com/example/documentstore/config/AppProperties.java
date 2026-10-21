package com.example.documentstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Wechat wechat,
        VirtualPayment virtualPayment,
        Catalog catalog,
        Ad ad,
        FreeDownload freeDownload,
        Security security
) {
    public record Wechat(String appId, String appSecret) {
    }

    public record VirtualPayment(
            String offerId,
            String appKey,
            int env,
            String currencyType,
            String mode,
            int priceFen,
            String callbackToken
    ) {
    }

    public record Catalog(
            String csvLocation,
            String imageDirectory,
            String imageUrlPrefix,
            long cacheTtlSeconds,
            int cacheMaxEntries
    ) {
    }

    public record Ad(
            int freeDownloadMaxPriceFen,
            String timeZone
    ) {
    }

    public record FreeDownload(
            boolean enabled,
            String timeZone
    ) {
    }

    public record Security(
            String jwtSecret,
            long jwtExpirationSeconds,
            String sessionKeyEncryptionSecret
    ) {
    }
}
