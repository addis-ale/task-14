package com.exam.system.dto.user;

import java.util.List;

public class UpdateUserRequest {

    private String username;
    private List<String> roles;
    private UserRequestScopeDto scopes;

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

    public UserRequestScopeDto getScopes() {
        return scopes;
    }

    public void setScopes(UserRequestScopeDto scopes) {
        this.scopes = scopes;
    }
}
