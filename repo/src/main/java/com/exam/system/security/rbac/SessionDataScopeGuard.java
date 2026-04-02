package com.exam.system.security.rbac;

import com.exam.system.dto.ScopeDto;
import com.exam.system.entity.ExamSession;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SessionDataScopeGuard {

    public void ensureAccessible(ExamSession session) {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Access denied");
        }
        if ("ADMIN".equalsIgnoreCase(context.getActiveRole()) || "ACADEMIC_AFFAIRS".equalsIgnoreCase(context.getActiveRole())) {
            return;
        }

        ScopeDto scope = context.getScope();
        if (scope == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Session out of data scope");
        }

        if (scope.getTermId() != null && !scope.getTermId().equals(session.getTermId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Session out of term scope");
        }
        if (!scope.getGradeIds().isEmpty() && !scope.getGradeIds().contains(session.getGradeId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Session out of grade scope");
        }
    }
}
