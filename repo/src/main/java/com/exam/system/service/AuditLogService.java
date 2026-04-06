package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.audit.AuditLogResponse;

public interface AuditLogService {

    PageData<AuditLogResponse> list(String action, String resource, Long userId, String from, String to, int page, int size);
}
