package com.exam.system.repository;

import com.exam.system.entity.RolePermission;
import com.exam.system.entity.RolePermissionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    Optional<RolePermission> findByRoleIdAndResourceAndAction(Long roleId, String resource, String action);

    List<RolePermission> findByRoleId(Long roleId);
}
