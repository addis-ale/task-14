package com.exam.system.audit;

import com.exam.system.entity.AuditLog;
import com.exam.system.repository.AuditLogRepository;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private final AuditLogRepository auditLogRepository;

    public AuditLogInterceptor(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        if (!request.getRequestURI().startsWith("/api/")) {
            return;
        }
        UserContext context = UserContextHolder.get();
        if (context == null) {
            return;
        }

        AuditLog log = new AuditLog();
        log.setUserId(context.getUserId());
        log.setAction(request.getMethod());
        log.setResource(request.getRequestURI());
        log.setResourceId(null);
        log.setIpAddress(request.getRemoteAddr());
        log.setDetailsJson("{\"status\":" + response.getStatus() + "}");
        auditLogRepository.save(log);
    }
}
