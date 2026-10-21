package com.example.documentstore.download;

import com.example.documentstore.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:free-download-disabled-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.catalog.image-url-prefix=/images",
        "app.wechat.app-id=test-app-id",
        "app.wechat.app-secret=test-app-secret",
        "app.virtual-payment.offer-id=test-offer",
        "app.virtual-payment.app-key=test-app-key",
        "app.virtual-payment.callback-token=test-callback-token",
        "app.free-download.enabled=false",
        "app.security.jwt-secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.session-key-encryption-secret=test-session-secret-16"
})
class FreeDownloadDisabledIntegrationTest {

    @Autowired
    private FreeDownloadService freeDownloadService;

    @Autowired
    private com.example.documentstore.repository.UserAccountRepository userRepository;

    @Test
    void shouldRejectWhenDisabled() {
        com.example.documentstore.domain.UserAccount user = new com.example.documentstore.domain.UserAccount();
        user.setOpenId("openid-disabled-free-download");
        user.setEncryptedSessionKey("encrypted-session-key");
        Long userId = userRepository.save(user).getId();

        assertThatThrownBy(() -> freeDownloadService.unlock(userId, "1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("限时免费活动已结束");
    }
}
