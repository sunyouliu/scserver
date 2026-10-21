package com.example.documentstore.repository;

import com.example.documentstore.domain.OrderStatus;
import com.example.documentstore.domain.VirtualOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VirtualOrderRepository extends JpaRepository<VirtualOrder, Long> {
    Optional<VirtualOrder> findByOutTradeNo(String outTradeNo);

    Optional<VirtualOrder> findByOutTradeNoAndUserId(String outTradeNo, Long userId);

    Optional<VirtualOrder> findTopByUserIdAndDocumentIdAndStatusOrderByPaidAtDesc(
            Long userId,
            String documentId,
            OrderStatus status
    );
}
