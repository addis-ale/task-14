package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.preference.NotificationPreferenceResponse;
import com.exam.system.dto.preference.NotificationPreferenceUpdateRequest;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final ActiveRoleGuard activeRoleGuard;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                            ActiveRoleGuard activeRoleGuard) {
        this.preferenceService = preferenceService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> get() {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(preferenceService.get(userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> update(
            @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(preferenceService.update(userId, request)));
    }
}
