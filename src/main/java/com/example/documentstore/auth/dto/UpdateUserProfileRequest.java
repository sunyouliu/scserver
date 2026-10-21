package com.example.documentstore.auth.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 64) String nickname,
        @Size(max = 1024) String avatarUrl
) {
}
