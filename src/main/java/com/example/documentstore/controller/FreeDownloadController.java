package com.example.documentstore.controller;

import com.example.documentstore.download.FreeDownloadService;
import com.example.documentstore.download.dto.FreeDownloadQuotaResponse;
import com.example.documentstore.download.dto.FreeDownloadResponse;
import com.example.documentstore.security.AuthInterceptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/free-downloads")
public class FreeDownloadController {

    private final FreeDownloadService freeDownloadService;

    public FreeDownloadController(FreeDownloadService freeDownloadService) {
        this.freeDownloadService = freeDownloadService;
    }

    @GetMapping("/{documentId}/link")
    public FreeDownloadResponse unlock(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId,
            @PathVariable String documentId
    ) {
        return freeDownloadService.unlock(userId, documentId);
    }

    @GetMapping("/today")
    public FreeDownloadQuotaResponse quota(@RequestAttribute(AuthInterceptor.CURRENT_USER_ID) Long userId) {
        return freeDownloadService.quota(userId);
    }
}
