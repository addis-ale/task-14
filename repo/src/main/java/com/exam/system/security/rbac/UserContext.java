package com.exam.system.security.rbac;

import com.exam.system.dto.ScopeDto;
import java.util.ArrayList;
import java.util.List;

public class UserContext {

    private Long userId;
    private String username;
    private List<String> roles = new ArrayList<>();
    private String activeRole;
    private ScopeDto scope;
    private String token;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getActiveRole() {
        return activeRole;
    }

    public void setActiveRole(String activeRole) {
        this.activeRole = activeRole;
    }

    public ScopeDto getScope() {
        return scope;
    }

    public void setScope(ScopeDto scope) {
        this.scope = scope;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
