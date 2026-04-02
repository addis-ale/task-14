package com.exam.system.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class SwitchRoleRequest {

    @NotBlank
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
