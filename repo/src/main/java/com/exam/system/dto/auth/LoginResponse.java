package com.exam.system.dto.auth;

public class LoginResponse {

    private String token;
    private String sessionSecret;
    private long expiresIn;
    private UserProfileDto user;
    private java.util.List<java.util.Map<String, String>> drafts = new java.util.ArrayList<>();

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSessionSecret() {
        return sessionSecret;
    }

    public void setSessionSecret(String sessionSecret) {
        this.sessionSecret = sessionSecret;
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

    public java.util.List<java.util.Map<String, String>> getDrafts() {
        return drafts;
    }

    public void setDrafts(java.util.List<java.util.Map<String, String>> drafts) {
        this.drafts = drafts;
    }
}
