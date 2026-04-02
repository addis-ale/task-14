package com.exam.system.repository;

import com.exam.system.entity.AntiCheatFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AntiCheatFlagRepository extends JpaRepository<AntiCheatFlag, Long> {

    Page<AntiCheatFlag> findByReviewStatus(String reviewStatus, Pageable pageable);

    long countByReviewStatus(String reviewStatus);
}
