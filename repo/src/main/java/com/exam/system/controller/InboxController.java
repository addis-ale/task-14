package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.inbox.InboxMessageResponse;
import com.exam.system.dto.inbox.UnreadCountResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.InboxService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inbox")
public class InboxController {

    private final InboxService inboxService;
    private final ActiveRoleGuard activeRoleGuard;

    public InboxController(InboxService inboxService, ActiveRoleGuard activeRoleGuard) {
        this.inboxService = inboxService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<InboxMessageResponse>>> list(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(inboxService.list(userId, read, page, size)));
    }

    @PutMapping("/{deliveryId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long deliveryId) {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        inboxService.markRead(userId, deliveryId);
        return ResponseEntity.ok(ApiResponse.successMessage("Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        inboxService.markAllRead(userId);
        return ResponseEntity.ok(ApiResponse.successMessage("All messages marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount() {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(new UnreadCountResponse(inboxService.unreadCount(userId))));
    }
}
