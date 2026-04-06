package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.dashboard.DashboardStatsResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ActiveRoleGuard activeRoleGuard;

    public DashboardController(DashboardService dashboardService, ActiveRoleGuard activeRoleGuard) {
        this.dashboardService = dashboardService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> stats(@RequestParam(required = false) Long termId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(termId)));
    }
}
