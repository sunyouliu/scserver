package com.example.documentstore.catalog;

import com.example.documentstore.config.CacheConfig;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.repository.DocumentAssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog-test;DB_CLOSE_DELAY=-1",
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
class CsvCatalogImporterIntegrationTest {

    @TempDir
    static Path imageDirectory;

    @DynamicPropertySource
    static void registerImageDirectory(DynamicPropertyRegistry registry) throws IOException {
        Files.writeString(imageDirectory.resolve("1.jpg"), "image");
        Files.writeString(imageDirectory.resolve("2.jpeg"), "image");
        Files.writeString(imageDirectory.resolve("3.png"), "image");
        registry.add("app.catalog.image-directory", imageDirectory::toString);
    }

    @Autowired
    private DocumentAssetRepository repository;

    @Autowired
    private DocumentCatalogService catalogService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldImportCsvIntoDatabase() {
        assertThat(repository.count()).isEqualTo(3);
        assertThat(repository.findById("1")).get()
                .extracting("title", "imageCount", "priceFen", "downloadLink")
                .containsExactly("城市建筑灵感图集", 12, 149, "https://pan.example.com/s/document-1");
    }

    @Test
    void shouldCacheHomePageAndServeServerImagePath() {
        var firstPage = catalogService.findPage("", 0, 20);

        assertThat(firstPage.items()).hasSize(3);
        assertThat(firstPage.items().get(0).thumbnailUrl()).isEqualTo("/images/1.jpg");
        assertThat(firstPage.items().get(0).detailImageUrl()).isEqualTo("/images/1.jpg");
        assertThat(firstPage.items().get(1).thumbnailUrl()).isEqualTo("/images/2.jpeg");
        assertThat(firstPage.items().get(2).thumbnailUrl()).isEqualTo("/images/3.png");
        assertThat(firstPage.items())
                .extracting("priceFen")
                .containsExactly(149, 50, 99);

        Cache cache = cacheManager.getCache(CacheConfig.DOCUMENT_PAGE_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.get(":0:20")).isNotNull();

        repository.save(cachedAsset("999", "缓存后新增文档"));
        assertThat(repository.count()).isEqualTo(4);
        assertThat(catalogService.findPage("", 0, 20).items()).hasSize(3);
        assertThat(catalogService.findPage("", 0, 100).items()).hasSize(4);
    }

    @Test
    void shouldSearchByTitleKeyword() {
        assertThat(catalogService.findPage("城市", 0, 20).items())
                .extracting("title")
                .containsExactly("城市建筑灵感图集");
        assertThat(catalogService.findPage("BUILD", 0, 20).items()).isEmpty();
    }

    private static DocumentAsset cachedAsset(String id, String title) {
        DocumentAsset asset = new DocumentAsset();
        asset.setId(id);
        asset.setSortOrder(999);
        asset.setTitle(title);
        asset.setImageCount(1);
        asset.setPriceFen(100);
        asset.setDownloadLink("https://pan.example.com/s/" + id);
        return asset;
    }
}
