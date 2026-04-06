package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.anticheat.AntiCheatFlagDetailResponse;
import com.exam.system.dto.anticheat.AntiCheatFlagSummaryResponse;
import com.exam.system.dto.anticheat.AntiCheatReviewRequest;
import com.exam.system.dto.common.PageData;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.AntiCheatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anti-cheat/flags")
public class AntiCheatController {

    private final AntiCheatService antiCheatService;
    private final ActiveRoleGuard activeRoleGuard;

    public AntiCheatController(AntiCheatService antiCheatService, ActiveRoleGuard activeRoleGuard) {
        this.antiCheatService = antiCheatService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<AntiCheatFlagSummaryResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(antiCheatService.list(status, page, size)));
    }

    @GetMapping("/{flagId}")
    public ResponseEntity<ApiResponse<AntiCheatFlagDetailResponse>> detail(@PathVariable Long flagId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(antiCheatService.detail(flagId)));
    }

    @PostMapping("/{flagId}/review")
    public ResponseEntity<ApiResponse<AntiCheatFlagDetailResponse>> review(@PathVariable Long flagId,
                                                                            @Valid @RequestBody AntiCheatReviewRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(antiCheatService.review(flagId, request)));
    }
}
