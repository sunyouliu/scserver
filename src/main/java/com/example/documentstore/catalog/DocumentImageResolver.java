package com.example.documentstore.catalog;

import com.example.documentstore.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 按文档序号在图片目录中查找真实文件，支持 jpg/jpeg/png/webp/gif/bmp 等多种格式。
 */
@Component
public class DocumentImageResolver {

    private static final Logger log = LoggerFactory.getLogger(DocumentImageResolver.class);
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif", "bmp");

    private final AppProperties properties;
    private final Map<String, String> imageFileNames = new HashMap<>();
    private volatile boolean loaded;

    public DocumentImageResolver(AppProperties properties) {
        this.properties = properties;
    }

    public String resolveFileName(String id) {
        ensureLoaded();
        return id == null ? null : imageFileNames.get(id);
    }

    public synchronized void refresh() {
        Map<String, String> scanned = new HashMap<>();
        Path directory = Path.of(properties.catalog().imageDirectory());
        if (Files.isDirectory(directory)) {
            try (Stream<Path> files = Files.list(directory)) {
                files.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String extension = extension(fileName);
                    if (extension.isEmpty() || !SUPPORTED_EXTENSIONS.contains(extension)) {
                        return;
                    }
                    String name = stripExtension(fileName);
                    if (name.isEmpty()) {
                        return;
                    }
                    scanned.merge(name, fileName, DocumentImageResolver::preferred);
                });
            } catch (IOException exception) {
                log.warn("读取图片目录失败：{}", directory, exception);
            }
        } else {
            log.warn("图片目录不存在：{}", directory.toAbsolutePath());
        }

        imageFileNames.clear();
        imageFileNames.putAll(scanned);
        loaded = true;
        log.info("已索引 {} 张本地图片，目录 {}", imageFileNames.size(), directory.toAbsolutePath().normalize());
    }

    private void ensureLoaded() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    refresh();
                }
            }
        }
    }

    private static String preferred(String current, String candidate) {
        return Comparator.comparingInt(DocumentImageResolver::extensionPriority)
                .compare(current, candidate) <= 0 ? current : candidate;
    }

    private static int extensionPriority(String fileName) {
        int index = SUPPORTED_EXTENSIONS.indexOf(extension(fileName));
        return index < 0 ? SUPPORTED_EXTENSIONS.size() : index;
    }

    private static String extension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 || dotIndex == fileName.length() - 1
                ? ""
                : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex < 0 ? fileName.trim() : fileName.substring(0, dotIndex).trim();
    }
}
