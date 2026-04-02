package com.exam.system.repository;

import com.exam.system.entity.ComplianceReview;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceReviewRepository extends JpaRepository<ComplianceReview, Long> {

    Page<ComplianceReview> findByStatus(String status, Pageable pageable);

    Optional<ComplianceReview> findByContentTypeAndContentId(String contentType, Long contentId);
}
