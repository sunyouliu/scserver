package com.example.documentstore.auth.dto;

public record WechatLoginResponse(
        String token,
        long expiresIn,
        UserProfileResponse user
) {
}
