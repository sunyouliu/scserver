package com.example.documentstore.catalog;

import com.example.documentstore.config.AppProperties;
import com.example.documentstore.config.OrderedConfiguration;
import com.example.documentstore.domain.DocumentAsset;
import com.example.documentstore.repository.DocumentAssetRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Order(OrderedConfiguration.SECOND)
public class CsvCatalogImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvCatalogImporter.class);
    private static final Pattern HEADER_PATTERN = Pattern.compile("序号|编号|图片数量|标题|名称|链接|价格|title|link|price", Pattern.CASE_INSENSITIVE);
    private static final int MIN_COLUMN_COUNT = 6;

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;
    private final DocumentAssetRepository repository;
    private final DocumentCacheManager documentCacheManager;
    private final DocumentImageResolver imageResolver;

    public CsvCatalogImporter(
            AppProperties properties,
            ResourceLoader resourceLoader,
            DocumentAssetRepository repository,
            DocumentCacheManager documentCacheManager,
            DocumentImageResolver imageResolver
    ) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.repository = repository;
        this.documentCacheManager = documentCacheManager;
        this.imageResolver = imageResolver;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Resource resource = resourceLoader.getResource(properties.catalog().csvLocation());
        if (!resource.exists()) {
            throw new IllegalStateException("CSV 文件不存在：" + properties.catalog().csvLocation());
        }

        List<DocumentAsset> assets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            int rowIndex = 0;
            for (CSVRecord record : CSVFormat.DEFAULT.parse(reader)) {
                if (record.size() < MIN_COLUMN_COUNT || isEmpty(record)) {
                    continue;
                }
                if (rowIndex == 0 && isHeader(record)) {
                    rowIndex += 1;
                    continue;
                }

                String id = stripBom(record.get(0)).trim();
                if (id.isEmpty()) {
                    id = String.valueOf(rowIndex);
                }

                DocumentAsset asset = repository.findById(id).orElseGet(DocumentAsset::new);
                asset.setId(id);
                asset.setSortOrder(parseSortOrder(id, rowIndex));
                asset.setNote(record.get(1).trim());
                asset.setTitle(defaultTitle(record.get(2), id));
                asset.setImageCount(parseImageCount(record.get(3)));
                asset.setPriceFen(parsePriceFen(record.get(4)));
                asset.setDownloadLink(record.get(5).trim());
                assets.add(asset);
                rowIndex += 1;
            }
        }

        repository.saveAll(assets);
        imageResolver.refresh();
        documentCacheManager.clearDocumentPages();
        log.info("已从 CSV 初始化 {} 条文档记录", assets.size());
    }

    private boolean isHeader(CSVRecord record) {
        String firstCell = stripBom(record.get(0)).trim();
        String cells = String.join("|", record.get(0), record.get(1), record.get(2), record.get(3), record.get(4), record.get(5));
        return !firstCell.matches("^\\d+$") && HEADER_PATTERN.matcher(cells).find();
    }

    private boolean isEmpty(CSVRecord record) {
        for (String value : record) {
            if (!value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private int parseSortOrder(String id, int fallback) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseImageCount(String value) {
        try {
            return Math.max(Integer.parseInt(value.trim()), 0);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private Integer parsePriceFen(String value) {
        String price = value == null ? "" : value.trim();
        if (price.isEmpty()) {
            return properties.virtualPayment().priceFen();
        }
        try {
            int priceFen = new BigDecimal(price).movePointRight(2).setScale(0, RoundingMode.HALF_UP).intValue();
            return Math.max(priceFen, 0);
        } catch (NumberFormatException ignored) {
            log.warn("无法解析价格 {}，使用默认价格", price);
            return properties.virtualPayment().priceFen();
        }
    }

    private String defaultTitle(String value, String id) {
        String title = value.trim();
        return title.isEmpty() ? "文档 " + id : title;
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}
