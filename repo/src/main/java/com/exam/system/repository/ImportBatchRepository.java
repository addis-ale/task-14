package com.exam.system.repository;

import com.exam.system.entity.ImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    Page<ImportBatch> findByEntityType(String entityType, Pageable pageable);
}
