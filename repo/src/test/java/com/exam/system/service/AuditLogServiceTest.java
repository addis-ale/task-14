package com.exam.system.service;

import com.exam.system.dto.audit.AuditLogResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.entity.AuditLog;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.service.impl.AuditLogServiceImpl;
import com.exam.system.service.impl.PageDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    private AuditLogServiceImpl auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, new PageDataBuilder());
    }

    @Test
    @DisplayName("List returns paginated audit logs")
    void listReturnsPaginatedLogs() {
        AuditLog log = createLog(1L, "LOGIN", "USER", "1");
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageData<AuditLogResponse> result = auditLogService.list(null, null, null, null, null, 1, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getAction()).isEqualTo("LOGIN");
    }

    @Test
    @DisplayName("List filters by action")
    void listFiltersByAction() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageData<AuditLogResponse> result = auditLogService.list("LOGIN", null, null, null, null, 1, 20);

        assertThat(result.getItems()).isEmpty();
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("List filters by date range")
    void listFiltersByDateRange() {
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageData<AuditLogResponse> result = auditLogService.list(null, null, null, "2026-01-01", "2026-12-31", 1, 20);

        assertThat(result.getItems()).isEmpty();
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private AuditLog createLog(Long id, String action, String resource, String resourceId) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setAction(action);
        log.setResource(resource);
        log.setResourceId(resourceId);
        log.setUserId(1L);
        log.setIpAddress("127.0.0.1");
        log.setTimestamp(LocalDateTime.now());
        return log;
    }
}
