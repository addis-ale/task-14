package com.exam.system.repository;

import com.exam.system.entity.AuditLogArchive;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogArchiveRepository extends JpaRepository<AuditLogArchive, Long> {
}
