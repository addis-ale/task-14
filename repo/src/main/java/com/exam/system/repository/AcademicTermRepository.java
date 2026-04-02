package com.exam.system.repository;

import com.exam.system.entity.AcademicTerm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {

    Optional<AcademicTerm> findByActiveTrue();
}
