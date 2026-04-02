package com.exam.system.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NotificationCreateRequest {

    @NotBlank
    private String eventType;

    @NotBlank
    private String title;

    @NotBlank
    private String body;

    @NotBlank
    private String priority;

    @NotNull
    private NotificationTargetScopeDto targetScope;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public NotificationTargetScopeDto getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(NotificationTargetScopeDto targetScope) {
        this.targetScope = targetScope;
    }
}
