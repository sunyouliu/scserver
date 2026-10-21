package com.example.documentstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_assets")
public class DocumentAsset {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(length = 512)
    private String note;

    @Column(nullable = false)
    private Integer imageCount;

    @Column
    private Integer priceFen;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 2048)
    private String downloadLink;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getImageCount() {
        return imageCount;
    }

    public void setImageCount(Integer imageCount) {
        this.imageCount = imageCount;
    }

    public Integer getPriceFen() {
        return priceFen;
    }

    public void setPriceFen(Integer priceFen) {
        this.priceFen = priceFen;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }
}
