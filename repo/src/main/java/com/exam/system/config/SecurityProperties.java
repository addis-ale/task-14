package com.exam.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private int replayWindowSeconds;
    private String hmacSecret;
    private String aesBase64Key;
    private int userRateLimitPerMinute;
    private int ipRateLimitPerMinute;
    private long requestSizeMaxBytes;
    private int sessionIdleTimeoutMinutes;
    private int rememberDeviceMaxDays;

    public int getReplayWindowSeconds() {
        return replayWindowSeconds;
    }

    public void setReplayWindowSeconds(int replayWindowSeconds) {
        this.replayWindowSeconds = replayWindowSeconds;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public void setHmacSecret(String hmacSecret) {
        this.hmacSecret = hmacSecret;
    }

    public String getAesBase64Key() {
        return aesBase64Key;
    }

    public void setAesBase64Key(String aesBase64Key) {
        this.aesBase64Key = aesBase64Key;
    }

    public int getUserRateLimitPerMinute() {
        return userRateLimitPerMinute;
    }

    public void setUserRateLimitPerMinute(int userRateLimitPerMinute) {
        this.userRateLimitPerMinute = userRateLimitPerMinute;
    }

    public int getIpRateLimitPerMinute() {
        return ipRateLimitPerMinute;
    }

    public void setIpRateLimitPerMinute(int ipRateLimitPerMinute) {
        this.ipRateLimitPerMinute = ipRateLimitPerMinute;
    }

    public long getRequestSizeMaxBytes() {
        return requestSizeMaxBytes;
    }

    public void setRequestSizeMaxBytes(long requestSizeMaxBytes) {
        this.requestSizeMaxBytes = requestSizeMaxBytes;
    }

    public int getSessionIdleTimeoutMinutes() {
        return sessionIdleTimeoutMinutes;
    }

    public void setSessionIdleTimeoutMinutes(int sessionIdleTimeoutMinutes) {
        this.sessionIdleTimeoutMinutes = sessionIdleTimeoutMinutes;
    }

    public int getRememberDeviceMaxDays() {
        return rememberDeviceMaxDays;
    }

    public void setRememberDeviceMaxDays(int rememberDeviceMaxDays) {
        this.rememberDeviceMaxDays = rememberDeviceMaxDays;
    }
}
