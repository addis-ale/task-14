package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.job.JobDetailResponse;
import com.exam.system.dto.job.JobSummaryResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.JobMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobMonitorController {

    private final JobMonitorService jobMonitorService;
    private final ActiveRoleGuard activeRoleGuard;

    public JobMonitorController(JobMonitorService jobMonitorService, ActiveRoleGuard activeRoleGuard) {
        this.jobMonitorService = jobMonitorService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<JobSummaryResponse>>> list(
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN");
        return ResponseEntity.ok(ApiResponse.success(jobMonitorService.list(jobType, status, page, size)));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> detail(@PathVariable Long jobId) {
        activeRoleGuard.requireAny("ADMIN");
        return ResponseEntity.ok(ApiResponse.success(jobMonitorService.detail(jobId)));
    }

    @PostMapping("/{jobId}/retry")
    public ResponseEntity<ApiResponse<Void>> retry(@PathVariable Long jobId) {
        activeRoleGuard.requireAny("ADMIN");
        jobMonitorService.retry(jobId);
        return ResponseEntity.ok(ApiResponse.successMessage("Job retry queued"));
    }
}
