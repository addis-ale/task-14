package com.exam.system.security.rbac;

import com.exam.system.entity.RolePermission;
import com.exam.system.entity.SysRole;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.RolePermissionRepository;
import com.exam.system.repository.SysRoleRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Evaluates RBAC permissions against the currently active role.
 */
@Component
public class PermissionEvaluatorService {

    private final SysRoleRepository roleRepository;
    private final RolePermissionRepository permissionRepository;

    public PermissionEvaluatorService(SysRoleRepository roleRepository,
                                      RolePermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public boolean hasPermission(UserContext context, String resource, String action) {
        if (context == null || context.getActiveRole() == null) {
            return false;
        }
        Optional<SysRole> role = roleRepository.findByName(context.getActiveRole());
        if (role.isEmpty()) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(context.getActiveRole())) {
            return true;
        }
        return permissionRepository
                .findByRoleIdAndResourceAndAction(role.get().getId(), resource, action)
                .map(RolePermission::getRoleId)
                .isPresent();
    }

    public void require(UserContext context, String resource, String action) {
        if (!hasPermission(context, resource, action)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
                    "Insufficient permissions for this action");
        }
    }
}
