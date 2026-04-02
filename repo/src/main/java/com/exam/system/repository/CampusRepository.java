package com.exam.system.repository;

import com.exam.system.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {
}
