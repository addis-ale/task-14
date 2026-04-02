package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.compliance.ComplianceDecisionRequest;
import com.exam.system.dto.compliance.ComplianceReviewDetailResponse;
import com.exam.system.dto.compliance.ComplianceReviewSummaryResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.ComplianceReviewService;
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
@RequestMapping("/api/v1/compliance-reviews")
public class ComplianceReviewController {

    private final ComplianceReviewService complianceReviewService;
    private final ActiveRoleGuard activeRoleGuard;

    public ComplianceReviewController(ComplianceReviewService complianceReviewService, ActiveRoleGuard activeRoleGuard) {
        this.complianceReviewService = complianceReviewService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<ComplianceReviewSummaryResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(complianceReviewService.list(status, page, size)));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ComplianceReviewDetailResponse>> detail(@PathVariable Long reviewId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(complianceReviewService.detail(reviewId)));
    }

    @PostMapping("/{reviewId}/approve")
    public ResponseEntity<ApiResponse<ComplianceReviewDetailResponse>> approve(@PathVariable Long reviewId,
                                                                               @Valid @RequestBody ComplianceDecisionRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(complianceReviewService.approve(reviewId, request.getComments())));
    }

    @PostMapping("/{reviewId}/reject")
    public ResponseEntity<ApiResponse<ComplianceReviewDetailResponse>> reject(@PathVariable Long reviewId,
                                                                              @Valid @RequestBody ComplianceDecisionRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(complianceReviewService.reject(reviewId, request.getComments())));
    }
}
