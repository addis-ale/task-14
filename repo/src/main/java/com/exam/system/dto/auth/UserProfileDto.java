package com.exam.system.dto.auth;

import com.exam.system.dto.ScopeDto;
import java.util.ArrayList;
import java.util.List;

public class UserProfileDto {

    private Long id;
    private String username;
    private List<String> roles = new ArrayList<>();
    private String activeRole;
    private ScopeDto scopes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public ScopeDto getScopes() {
        return scopes;
    }

    public void setScopes(ScopeDto scopes) {
        this.scopes = scopes;
    }
}
