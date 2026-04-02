package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.subject.SubjectRequest;
import com.exam.system.dto.subject.SubjectResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final ActiveRoleGuard activeRoleGuard;

    public SubjectController(SubjectService subjectService, ActiveRoleGuard activeRoleGuard) {
        this.subjectService = subjectService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<SubjectResponse>>> listSubjects(
            @RequestParam(required = false) Long gradeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(subjectService.listSubjects(gradeId, page, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@Valid @RequestBody SubjectRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(subjectService.createSubject(request)));
    }

    @PutMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(@PathVariable Long subjectId,
                                                                      @Valid @RequestBody SubjectRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(subjectService.updateSubject(subjectId, request)));
    }

    @DeleteMapping("/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long subjectId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        subjectService.deleteSubject(subjectId);
        return ResponseEntity.noContent().build();
    }
}
