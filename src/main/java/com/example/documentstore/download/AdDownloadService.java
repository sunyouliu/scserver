package com.example.documentstore.download;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.config.AppProperties;
import com.example.documentstore.domain.AdDownloadUsage;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.download.dto.AdDownloadResponse;
import com.example.documentstore.download.dto.AdQuotaResponse;
import com.example.documentstore.repository.AdDownloadUsageRepository;
import com.example.documentstore.repository.DocumentAssetRepository;
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
public class AdDownloadService {

    private static final Logger log = LoggerFactory.getLogger(AdDownloadService.class);
    private static final int DAILY_LIMIT = 1;

    private final AppProperties properties;
    private final DocumentAssetRepository documentRepository;
    private final AdDownloadUsageRepository usageRepository;
    private final UserAccountRepository userRepository;

    public AdDownloadService(
            AppProperties properties,
            DocumentAssetRepository documentRepository,
            AdDownloadUsageRepository usageRepository,
            UserAccountRepository userRepository
    ) {
        this.properties = properties;
        this.documentRepository = documentRepository;
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AdDownloadResponse unlock(Long userId, String documentId) {
        UserAccount user = userRepository.findById(userId).orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "登录用户不存在"));
        DocumentAsset document = documentRepository.findById(documentId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "文档不存在"));

        if (resolvePriceFen(document) > properties.ad().freeDownloadMaxPriceFen()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AD_DOWNLOAD_NOT_ELIGIBLE", "该文档不支持看广告免费下载");
        }

        LocalDate today = currentDate();
        if (usageRepository.countByUserIdAndUsageDate(user.getId(), today) >= DAILY_LIMIT) {
            throw quotaExhausted();
        }

        AdDownloadUsage usage = new AdDownloadUsage();
        usage.setUserId(user.getId());
        usage.setDocumentId(document.getId());
        usage.setUsageDate(today);
        usage.setCreatedAt(Instant.now());
        try {
            usageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException exception) {
            throw quotaExhausted();
        }

        log.info("用户 {} 通过广告免费解锁文档 {}", user.getId(), document.getId());
        return new AdDownloadResponse("UNLOCKED", document.getId(), document.getDownloadLink());
    }

    @Transactional(readOnly = true)
    public AdQuotaResponse quota(Long userId) {
        LocalDate today = currentDate();
        int used = (int) usageRepository.countByUserIdAndUsageDate(userId, today);
        return new AdQuotaResponse(today.toString(), DAILY_LIMIT, used, Math.max(DAILY_LIMIT - used, 0));
    }

    private int resolvePriceFen(DocumentAsset document) {
        Integer priceFen = document.getPriceFen();
        return priceFen != null && priceFen > 0 ? priceFen : properties.virtualPayment().priceFen();
    }

    private LocalDate currentDate() {
        return LocalDate.now(ZoneId.of(properties.ad().timeZone()));
    }

    private ApiException quotaExhausted() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AD_DOWNLOAD_QUOTA_EXHAUSTED", "每人每天仅可看广告免费下载一次");
    }
}
