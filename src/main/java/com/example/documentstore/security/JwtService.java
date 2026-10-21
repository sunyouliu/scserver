package com.example.documentstore.security;

import com.example.documentstore.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final AppProperties properties;
    private final SecretKey signingKey;

    public JwtService(AppProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(requireSecret(properties.security().jwtSecret()).getBytes(StandardCharsets.UTF_8));
    }

    private static String requireSecret(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 未配置，请在 server/config/app.properties 或环境变量中设置至少 32 个字符的随机密钥");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET 长度不足，至少需要 32 个字符（256 位）");
        }
        return jwtSecret;
    }

    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.security().jwtExpirationSeconds())))
                .signWith(signingKey)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
