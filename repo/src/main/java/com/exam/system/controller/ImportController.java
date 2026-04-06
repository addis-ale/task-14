package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.importexport.ImportBatchResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.ImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ImportController {

    private final ImportService importService;
    private final ActiveRoleGuard activeRoleGuard;

    public ImportController(ImportService importService, ActiveRoleGuard activeRoleGuard) {
        this.importService = importService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @PostMapping("/import/upload")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.status(201).body(ApiResponse.success(importService.upload(file, entityType, userId)));
    }

    @PostMapping("/import/{batchId}/commit")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> commit(@PathVariable Long batchId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(importService.commit(batchId, userId)));
    }

    @PostMapping("/import/{batchId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long batchId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        Long userId = UserContextHolder.get().getUserId();
        importService.cancel(batchId, userId);
        return ResponseEntity.ok(ApiResponse.successMessage("Import cancelled"));
    }

    @GetMapping("/import/{batchId}")
    public ResponseEntity<ApiResponse<ImportBatchResponse>> detail(@PathVariable Long batchId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(importService.detail(batchId)));
    }

    @GetMapping("/import")
    public ResponseEntity<ApiResponse<PageData<ImportBatchResponse>>> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(importService.list(entityType, page, size)));
    }

    @GetMapping("/export/{entityType}")
    public ResponseEntity<byte[]> export(@PathVariable String entityType,
                                          @RequestParam(required = false) Long termId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        byte[] data = importService.export(entityType, termId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + entityType + "_export.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
