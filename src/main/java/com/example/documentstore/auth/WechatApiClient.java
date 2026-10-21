package com.example.documentstore.auth;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Component
public class WechatApiClient {

    private static final Logger log = LoggerFactory.getLogger(WechatApiClient.class);

    private final AppProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WechatApiClient(AppProperties properties, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder.requestFactory(requestFactory()).build();
        this.objectMapper = objectMapper;
    }

    private static ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    public WechatSession exchangeCode(String code) {
        String appId = properties.wechat().appId();
        String appSecret = properties.wechat().appSecret();
        if (isBlank(appId) || isBlank(appSecret)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WECHAT_NOT_CONFIGURED", "服务端尚未配置微信 AppID 和 AppSecret");
        }

        String uri = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode()
                .toUriString();

        try {
            String responseBody = restClient.get().uri(uri).retrieve().body(String.class);
            JsonNode response = objectMapper.readTree(responseBody);
            if (response.has("errcode") && response.path("errcode").asInt() != 0) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "WECHAT_LOGIN_FAILED",
                        "微信登录失败：" + response.path("errmsg").asText("未知错误")
                );
            }

            String openId = response.path("openid").asText("");
            String sessionKey = response.path("session_key").asText("");
            if (openId.isBlank() || sessionKey.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "INVALID_WECHAT_RESPONSE", "微信登录响应缺少 openid 或 session_key");
            }
            return new WechatSession(openId, response.path("unionid").asText(null), sessionKey);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("调用微信 jscode2session 失败", exception);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_API_ERROR", "暂时无法连接微信登录服务");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record WechatSession(String openId, String unionId, String sessionKey) {
    }
}
