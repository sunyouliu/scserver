package com.example.documentstore.auth.dto;

public record UserProfileResponse(
        Long id,
        String nickname,
        String avatarUrl
) {
}
