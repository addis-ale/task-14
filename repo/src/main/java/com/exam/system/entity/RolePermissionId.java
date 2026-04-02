package com.exam.system.entity;

import java.io.Serializable;
import java.util.Objects;

public class RolePermissionId implements Serializable {

    private Long roleId;
    private String resource;
    private String action;

    public RolePermissionId() {
    }

    public RolePermissionId(Long roleId, String resource, String action) {
        this.roleId = roleId;
        this.resource = resource;
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RolePermissionId that)) {
            return false;
        }
        return Objects.equals(roleId, that.roleId)
                && Objects.equals(resource, that.resource)
                && Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, resource, action);
    }
}
