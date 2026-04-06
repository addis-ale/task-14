package com.exam.system.service.impl;

import com.exam.system.dto.audit.AuditLogResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.entity.AuditLog;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.service.AuditLogService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PageDataBuilder pageDataBuilder;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository, PageDataBuilder pageDataBuilder) {
        this.auditLogRepository = auditLogRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<AuditLogResponse> list(String action, String resource, Long userId, String from, String to, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size), Sort.by(Sort.Direction.DESC, "timestamp"));

        Specification<AuditLog> spec = Specification.where(null);

        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (resource != null && !resource.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("resource"), resource));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (from != null && !from.isBlank()) {
            LocalDateTime fromDateTime = LocalDate.parse(from).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), fromDateTime));
        }
        if (to != null && !to.isBlank()) {
            LocalDateTime toDateTime = LocalDate.parse(to).atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), toDateTime));
        }

        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageable);
        return pageDataBuilder.from(logs, this::toResponse, page, size);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setUserId(log.getUserId());
        response.setAction(log.getAction());
        response.setResource(log.getResource());
        response.setResourceId(log.getResourceId());
        response.setIpAddress(log.getIpAddress());
        response.setTimestamp(log.getTimestamp());
        response.setDetailsJson(log.getDetailsJson());
        return response;
    }
}
