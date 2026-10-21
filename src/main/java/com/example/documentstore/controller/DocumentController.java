package com.example.documentstore.controller;

import com.example.documentstore.catalog.DocumentCatalogService;
import com.example.documentstore.catalog.dto.DocumentPageResponse;
import com.example.documentstore.catalog.dto.DocumentResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentCatalogService catalogService;

    public DocumentController(DocumentCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public DocumentPageResponse findPage(
            @RequestParam(defaultValue = "") @Size(max = 64) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size
    ) {
        return catalogService.findPage(keyword, page, size);
    }

    @GetMapping("/{id}")
    public DocumentResponse findById(@PathVariable String id) {
        return catalogService.findById(id);
    }

}
