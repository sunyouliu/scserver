package com.example.documentstore.download;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.domain.FreeDownloadUsage;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.download.dto.FreeDownloadQuotaResponse;
import com.example.documentstore.download.dto.FreeDownloadResponse;
import com.example.documentstore.repository.DocumentAssetRepository;
import com.example.documentstore.repository.FreeDownloadUsageRepository;
import com.example.documentstore.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class FreeDownloadService {

    private static final Logger log = LoggerFactory.getLogger(FreeDownloadService.class);
    private static final int DAILY_LIMIT = 1;

    private final AppProperties properties;
    private final DocumentAssetRepository documentRepository;
    private final FreeDownloadUsageRepository usageRepository;
    private final UserAccountRepository userRepository;

    public FreeDownloadService(
            AppProperties properties,
            DocumentAssetRepository documentRepository,
            FreeDownloadUsageRepository usageRepository,
            UserAccountRepository userRepository
    ) {
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FreeDownloadResponse unlock(Long userId, String documentId) {
        if (!properties.freeDownload().enabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FREE_DOWNLOAD_DISABLED", "限时免费活动已结束");
        }

        UserAccount user = userRepository.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "登录用户不存在"));
        DocumentAsset document = documentRepository.findById(documentId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "文档不存在"));

        LocalDate today = currentDate();
        if (usageRepository.countByUserIdAndUsageDate(user.getId(), today) >= DAILY_LIMIT) {
            throw quotaExhausted();
        }

        FreeDownloadUsage usage = new FreeDownloadUsage();
        usage.setUserId(user.getId());
        usage.setDocumentId(document.getId());
        usage.setUsageDate(today);
        usage.setCreatedAt(Instant.now());
        try {
            usageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException exception) {
            throw quotaExhausted();
        }

        log.info("用户 {} 限时免费下载文档 {}", user.getId(), document.getId());
        return new FreeDownloadResponse("UNLOCKED", document.getId(), document.getDownloadLink());
    }

    @Transactional(readOnly = true)
    public FreeDownloadQuotaResponse quota(Long userId) {
        LocalDate today = currentDate();
        int used = (int) usageRepository.countByUserIdAndUsageDate(userId, today);
        return new FreeDownloadQuotaResponse(today.toString(), DAILY_LIMIT, used, Math.max(DAILY_LIMIT - used, 0));
    }

    private LocalDate currentDate() {
        return LocalDate.now(ZoneId.of(properties.freeDownload().timeZone()));
    }

    private ApiException quotaExhausted() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "FREE_DOWNLOAD_QUOTA_EXHAUSTED", "每人每天仅可限时免费下载一次");
    }
}
