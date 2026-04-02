package com.exam.system.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private Boolean rememberDevice = Boolean.FALSE;

    private String deviceFingerprint;

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

    public Boolean getRememberDevice() {
        return rememberDevice;
    }

    public void setRememberDevice(Boolean rememberDevice) {
        this.rememberDevice = rememberDevice;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }
}
