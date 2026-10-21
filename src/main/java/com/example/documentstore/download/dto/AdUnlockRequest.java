package com.example.documentstore.download.dto;

import jakarta.validation.constraints.NotBlank;

public record AdUnlockRequest(@NotBlank String documentId) {
}
