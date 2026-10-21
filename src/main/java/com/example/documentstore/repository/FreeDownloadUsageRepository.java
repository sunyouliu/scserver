package com.example.documentstore.repository;

import com.example.documentstore.domain.FreeDownloadUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface FreeDownloadUsageRepository extends JpaRepository<FreeDownloadUsage, Long> {

    long countByUserIdAndUsageDate(Long userId, LocalDate usageDate);
}
