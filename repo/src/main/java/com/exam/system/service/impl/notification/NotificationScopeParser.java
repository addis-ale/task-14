package com.exam.system.service.impl.notification;

import com.exam.system.dto.notification.NotificationTargetScopeDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationScopeParser {

    private final ObjectMapper objectMapper;

    public NotificationScopeParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(NotificationTargetScopeDto scope) {
        try {
            return objectMapper.writeValueAsString(scope);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize target scope", ex);
        }
    }

    public NotificationTargetScopeDto fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new NotificationTargetScopeDto();
        }
        try {
            return objectMapper.readValue(json, NotificationTargetScopeDto.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse target scope", ex);
        }
    }
}
