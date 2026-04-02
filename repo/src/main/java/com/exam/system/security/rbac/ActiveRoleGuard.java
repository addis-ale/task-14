package com.exam.system.security.rbac;

import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ActiveRoleGuard {

    public void requireAny(String... allowedRoles) {
        UserContext context = UserContextHolder.get();
        if (context == null || context.getActiveRole() == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Access denied");
        }
        Set<String> allowed = Arrays.stream(allowedRoles)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (!allowed.contains(context.getActiveRole().toUpperCase())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Insufficient active role permissions");
        }
    }

    public boolean isAny(String... allowedRoles) {
        UserContext context = UserContextHolder.get();
        if (context == null || context.getActiveRole() == null) {
            return false;
        }
        String activeRole = context.getActiveRole().toUpperCase();
        for (String role : allowedRoles) {
            if (activeRole.equals(role.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
