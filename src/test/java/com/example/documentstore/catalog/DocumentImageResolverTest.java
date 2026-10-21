package com.example.documentstore.catalog;

import com.example.documentstore.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentImageResolverTest {

    @Test
    void shouldResolveRealFileNamesWithMixedExtensions(@TempDir Path imageDirectory) throws IOException {
        Files.writeString(imageDirectory.resolve("1.jpg"), "a");
        Files.writeString(imageDirectory.resolve("2.jpeg"), "b");
        Files.writeString(imageDirectory.resolve("3.PNG"), "c");
        Files.writeString(imageDirectory.resolve("4.webp"), "d");
        Files.writeString(imageDirectory.resolve("notes.txt"), "ignored");

        DocumentImageResolver resolver = resolverFor(imageDirectory);

        assertThat(resolver.resolveFileName("1")).isEqualTo("1.jpg");
        assertThat(resolver.resolveFileName("2")).isEqualTo("2.jpeg");
        assertThat(resolver.resolveFileName("3")).isEqualTo("3.PNG");
        assertThat(resolver.resolveFileName("4")).isEqualTo("4.webp");
        assertThat(resolver.resolveFileName("5")).isNull();
        assertThat(resolver.resolveFileName("notes")).isNull();
    }

    @Test
    void shouldPreferJpgWhenSameIdHasMultipleFormats(@TempDir Path imageDirectory) throws IOException {
        Files.writeString(imageDirectory.resolve("7.png"), "a");
        Files.writeString(imageDirectory.resolve("7.jpg"), "b");

        assertThat(resolverFor(imageDirectory).resolveFileName("7")).isEqualTo("7.jpg");
    }

    @Test
    void shouldReturnNullWhenDirectoryMissing(@TempDir Path imageDirectory) {
        assertThat(resolverFor(imageDirectory.resolve("missing")).resolveFileName("1")).isNull();
    }

    private static DocumentImageResolver resolverFor(Path imageDirectory) {
        AppProperties properties = new AppProperties(
                new AppProperties.Wechat("app-id", "app-secret"),
                new AppProperties.VirtualPayment("offer", "key", 1, "CNY", "short_series_goods", 100, "token"),
                new AppProperties.Catalog("classpath:res.csv", imageDirectory.toString(), "/images", 3600L, 200),
                new AppProperties.Ad(50, "Asia/Shanghai"),
                new AppProperties.FreeDownload(true, "Asia/Shanghai"),
                new AppProperties.Security("jwt-secret-with-at-least-32-characters", 604800L, "session-secret-16")
        );
        return new DocumentImageResolver(properties);
    }
}
