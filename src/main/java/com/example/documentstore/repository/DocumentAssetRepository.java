package com.example.documentstore.repository;

import com.example.documentstore.domain.DocumentAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, String> {

    @Query("""
            select d from DocumentAsset d
            where (:keyword is null or :keyword = '' or lower(d.title) like lower(concat('%', :keyword, '%')))
            """)
    Page<DocumentAsset> search(@Param("keyword") String keyword, Pageable pageable);
}
