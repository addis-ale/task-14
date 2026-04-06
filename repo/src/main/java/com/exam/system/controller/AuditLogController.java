package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.audit.AuditLogResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final ActiveRoleGuard activeRoleGuard;

    public AuditLogController(AuditLogService auditLogService, ActiveRoleGuard activeRoleGuard) {
        this.auditLogService = auditLogService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<AuditLogResponse>>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(auditLogService.list(action, resource, userId, from, to, page, size)));
    }
}
