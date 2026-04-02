package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.version.VersionCompareResponse;
import com.exam.system.dto.version.VersionDetailResponse;
import com.exam.system.dto.version.VersionSummaryResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.VersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/versions")
public class VersionController {

    private final VersionService versionService;
    private final ActiveRoleGuard activeRoleGuard;

    public VersionController(VersionService versionService, ActiveRoleGuard activeRoleGuard) {
        this.versionService = versionService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<VersionSummaryResponse>>> listVersions(
            @RequestParam String entityType,
            @RequestParam Long entityId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(versionService.listVersions(entityType, entityId, page, size)));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<ApiResponse<VersionDetailResponse>> getVersion(@PathVariable Long versionId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(versionService.getVersion(versionId)));
    }

    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<VersionCompareResponse>> compare(@RequestParam Long versionA,
                                                                       @RequestParam Long versionB) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(versionService.compare(versionA, versionB)));
    }

    @PostMapping("/{versionId}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long versionId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        versionService.restore(versionId);
        return ResponseEntity.ok(ApiResponse.successMessage("Version restored"));
    }
}
