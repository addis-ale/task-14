package com.exam.system.dto.auth;

public class LoginResponse {

    private String token;
    private long expiresIn;
    private UserProfileDto user;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserProfileDto getUser() {
        return user;
    }

    public void setUser(UserProfileDto user) {
        this.user = user;
    }
}
