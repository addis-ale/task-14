package com.exam.system.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotEmpty
    private List<String> roles = new ArrayList<>();

    private UserRequestScopeDto scopes;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
