package com.example.documentstore.download;

import com.example.documentstore.common.ApiException;
import com.example.documentstore.domain.UserAccount;
import com.example.documentstore.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ad-download-test;DB_CLOSE_DELAY=-1",
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
class AdDownloadServiceIntegrationTest {

    @Autowired
    private AdDownloadService adDownloadService;

    @Autowired
    private UserAccountRepository userRepository;

    @Test
    void shouldUnlockOncePerDayAndRejectSecondUnlock() {
        Long userId = saveUser("ad-user-1").getId();

        assertThat(adDownloadService.quota(userId).remaining()).isEqualTo(1);

        var unlocked = adDownloadService.unlock(userId, "2");
        assertThat(unlocked.status()).isEqualTo("UNLOCKED");
        assertThat(unlocked.downloadLink()).isEqualTo("https://pan.example.com/s/document-2");

        assertThat(adDownloadService.quota(userId).remaining()).isZero();
        assertThatThrownBy(() -> adDownloadService.unlock(userId, "2"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("每人每天仅可看广告免费下载一次");
    }

    @Test
    void shouldRejectDocumentPricedAboveFreeLimit() {
        Long userId = saveUser("ad-user-2").getId();

        assertThatThrownBy(() -> adDownloadService.unlock(userId, "1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("不支持看广告免费下载");
    }

    private UserAccount saveUser(String openId) {
        UserAccount user = new UserAccount();
        user.setOpenId(openId);
        user.setEncryptedSessionKey("encrypted-session-key");
        return userRepository.save(user);
    }
}
