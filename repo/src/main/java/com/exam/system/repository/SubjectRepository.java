package com.exam.system.repository;

import com.exam.system.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Page<Subject> findByGradeId(Long gradeId, Pageable pageable);
}
