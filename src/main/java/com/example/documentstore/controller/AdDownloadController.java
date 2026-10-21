package com.example.documentstore.controller;

import com.example.documentstore.download.AdDownloadService;
import com.example.documentstore.download.dto.AdDownloadResponse;
import com.example.documentstore.download.dto.AdQuotaResponse;
import com.example.documentstore.download.dto.AdUnlockRequest;
import com.example.documentstore.security.AuthInterceptor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mini-program/ad-downloads")
public class AdDownloadController {

    private final AdDownloadService adDownloadService;

    public AdDownloadController(AdDownloadService adDownloadService) {
        this.adDownloadService = adDownloadService;
    }

    @PostMapping
    public AdDownloadResponse unlock(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId,
            @Valid @RequestBody AdUnlockRequest request
    ) {
        return adDownloadService.unlock(userId, request.documentId());
    }

    @GetMapping("/today")
    public AdQuotaResponse quota(@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId) {
        return adDownloadService.quota(userId);
    }
}
