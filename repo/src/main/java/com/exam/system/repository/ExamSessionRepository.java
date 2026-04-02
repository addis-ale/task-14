package com.exam.system.repository;

import com.exam.system.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long>, JpaSpecificationExecutor<ExamSession> {

    long countByTermId(Long termId);
}
