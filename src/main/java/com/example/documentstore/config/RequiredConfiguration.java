package com.example.documentstore.config;

import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * 在 Spring 容器刷新之前校验必填配置，避免缺少密钥时抛出难以理解的异常栈。
 */
public final class RequiredConfiguration {

    private static final int MIN_JWT_SECRET_LENGTH = 32;
    private static final int MIN_SESSION_KEY_SECRET_LENGTH = 16;

    private RequiredConfiguration() {
    }

    public static void validate(Environment environment) {
        List<String> missing = new ArrayList<>();
        require(missing, environment, "app.wechat.app-id", "WECHAT_APP_ID", 1);
        require(missing, environment, "app.wechat.app-secret", "WECHAT_APP_SECRET", 1);
        // 回调 Token 只在支付回调时校验，缺失时拒绝回调而不是阻止启动。
        require(missing, environment, "app.security.jwt-secret", "JWT_SECRET", MIN_JWT_SECRET_LENGTH);
        require(missing, environment, "app.security.session-key-encryption-secret",
                "SESSION_KEY_ENCRYPTION_SECRET", MIN_SESSION_KEY_SECRET_LENGTH);

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "缺少或无效的服务端配置：" + String.join("、", missing)
                            + "。可在 server/config/app.properties 中填写，或设置同名环境变量后重启。"
            );
        }
        checkDatabase(environment);
    }

    private static void checkDatabase(Environment environment) {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password", "");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("DB_URL 未配置，请在 server/config/app.properties 中设置数据库连接地址");
        }

        DriverManager.setLoginTimeout(5);
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.isValid(2);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "数据库无法连接，请检查 DB_URL、账号密码与数据库是否已启动；"
                            + "若使用 H2 文件库，确认没有其他服务实例正在占用同一数据库文件。"
                            + "原始错误：" + exception.getMessage(),
                    exception
            );
        }
    }

    private static void require(
            List<String> missing,
            Environment environment,
            String property,
            String environmentName,
            int minLength
    ) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            missing.add(environmentName + "（未配置）");
            return;
        }
        if (value.length() < minLength) {
            missing.add(String.format("%s（至少 %d 个字符）", environmentName, minLength));
        }
    }
}
