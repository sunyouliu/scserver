package com.example.documentstore.repository;

import com.example.documentstore.domain.AdDownloadUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AdDownloadUsageRepository extends JpaRepository<AdDownloadUsage, Long> {

    long countByUserIdAndUsageDate(Long userId, LocalDate usageDate);
}
