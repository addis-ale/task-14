package com.exam.system.repository;

import com.exam.system.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByTimestampBefore(LocalDateTime threshold);

    void deleteByTimestampBefore(LocalDateTime threshold);
}
