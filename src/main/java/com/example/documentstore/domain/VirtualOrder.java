package com.example.documentstore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "virtual_orders")
public class VirtualOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String outTradeNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 64)
    private String documentId;

    @Column(nullable = false)
    private Integer amountFen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(length = 128)
    private String platformOrderNo;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant paidAt;

    @jakarta.persistence.Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (version == null) {
            version = 0L;
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }

    public Long getId() {
        return id;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Integer getAmountFen() {
        return amountFen;
    }

    public void setAmountFen(Integer amountFen) {
        this.amountFen = amountFen;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getPlatformOrderNo() {
        return platformOrderNo;
    }

    public void setPlatformOrderNo(String platformOrderNo) {
        this.platformOrderNo = platformOrderNo;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void markPaid(String platformOrderNo) {
        status = OrderStatus.PAID;
        this.platformOrderNo = platformOrderNo;
        paidAt = Instant.now();
    }
}
