package com.example.documentstore.download;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.download.dto.FreeDownloadQuotaResponse;
import com.example.documentstore.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:free-download-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.catalog.image-url-prefix=/images",
        "app.wechat.app-id=test-app-id",
        "app.wechat.app-secret=test-app-secret",
        "app.virtual-payment.offer-id=test-offer",
        "app.virtual-payment.app-key=test-app-key",
        "app.virtual-payment.callback-token=test-callback-token",
        "app.security.jwt-secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.session-key-encryption-secret=test-session-secret-16"
})
class FreeDownloadServiceIntegrationTest {

    @Autowired
    private FreeDownloadService freeDownloadService;

    @Autowired
    private UserAccountRepository userRepository;

    @Test
    void shouldLimitOneFreeDownloadPerDay() {
        Long userId = createUser("openid-free-download");

        var first = freeDownloadService.unlock(userId, "1");
        assertThat(first.status()).isEqualTo("UNLOCKED");
        assertThat(first.documentId()).isEqualTo("1");
        assertThat(first.downloadLink()).isEqualTo("https://pan.example.com/s/document-1");

        FreeDownloadQuotaResponse quota = freeDownloadService.quota(userId);
        assertThat(quota.limit()).isEqualTo(1);
        assertThat(quota.used()).isEqualTo(1);
        assertThat(quota.remaining()).isZero();

        assertThatThrownBy(() -> freeDownloadService.unlock(userId, "2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("每人每天仅可限时免费下载一次");
    }

    @Test
    void shouldNotConsumeQuotaWhenDocumentMissing() {
        Long userId = createUser("openid-missing-document");

        assertThatThrownBy(() -> freeDownloadService.unlock(userId, "not-exist"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("文档不存在");

        assertThat(freeDownloadService.quota(userId).remaining()).isEqualTo(1);
    }

    private Long createUser(String openId) {
        UserAccount user = new UserAccount();
        user.setOpenId(openId);
        user.setEncryptedSessionKey("encrypted-session-key");
        return userRepository.save(user).getId();
    }
}
