package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.notification.DeliveryResponse;
import com.exam.system.dto.notification.NotificationCreateRequest;
import com.exam.system.dto.notification.NotificationDetailResponse;
import com.exam.system.dto.notification.NotificationSummaryResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final ActiveRoleGuard activeRoleGuard;

    public NotificationController(NotificationService notificationService, ActiveRoleGuard activeRoleGuard) {
        this.notificationService = notificationService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<NotificationSummaryResponse>>> list(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(notificationService.list(eventType, status, page, size)));
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> detail(@PathVariable Long notificationId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(notificationService.detail(notificationId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationSummaryResponse>> create(@Valid @RequestBody NotificationCreateRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(notificationService.create(request)));
    }

    @PostMapping("/{notificationId}/submit-review")
    public ResponseEntity<ApiResponse<Void>> submitReview(@PathVariable Long notificationId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        notificationService.submitReview(notificationId);
        return ResponseEntity.ok(ApiResponse.successMessage("Submitted for compliance review"));
    }

    @PostMapping("/{notificationId}/publish")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable Long notificationId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        notificationService.publish(notificationId);
        return ResponseEntity.ok(ApiResponse.successMessage("Notification publishing started"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> delete(@PathVariable Long notificationId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        notificationService.delete(notificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{notificationId}/deliveries")
    public ResponseEntity<ApiResponse<PageData<DeliveryResponse>>> deliveries(@PathVariable Long notificationId,
                                                                              @RequestParam(required = false) String status,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(notificationService.listDeliveries(notificationId, status, page, size)));
    }

    @PostMapping("/{notificationId}/deliveries/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long notificationId, @PathVariable Long deliveryId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        notificationService.retryDelivery(notificationId, deliveryId);
        return ResponseEntity.ok(ApiResponse.successMessage("Delivery retry triggered"));
    }
}
