package com.exam.system.dto.auth;

import com.exam.system.dto.ScopeDto;

public class SwitchRoleResponse {

    private String activeRole;
    private ScopeDto scopes;

    public String getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(String activeRole) {
        this.activeRole = activeRole;
    }

    public ScopeDto getScopes() {
        return scopes;
    }

    public void setScopes(ScopeDto scopes) {
        this.scopes = scopes;
    }
}
