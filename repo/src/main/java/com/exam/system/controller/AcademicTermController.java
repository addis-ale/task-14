package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.term.TermRequest;
import com.exam.system.dto.term.TermResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.service.AcademicTermService;
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
@RequestMapping("/api/v1/terms")
public class AcademicTermController {

    private final AcademicTermService academicTermService;
    private final ActiveRoleGuard activeRoleGuard;

    public AcademicTermController(AcademicTermService academicTermService, ActiveRoleGuard activeRoleGuard) {
        this.academicTermService = academicTermService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageData<TermResponse>>> listTerms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(academicTermService.listTerms(page, size)));
    }

    @GetMapping("/{termId}")
    public ResponseEntity<ApiResponse<TermResponse>> getTerm(@PathVariable Long termId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(academicTermService.getTerm(termId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TermResponse>> createTerm(@Valid @RequestBody TermRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(academicTermService.createTerm(request)));
    }

    @PutMapping("/{termId}")
    public ResponseEntity<ApiResponse<TermResponse>> updateTerm(@PathVariable Long termId,
                                                                @Valid @RequestBody TermRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(academicTermService.updateTerm(termId, request)));
    }

    @DeleteMapping("/{termId}")
    public ResponseEntity<Void> deleteTerm(@PathVariable Long termId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        academicTermService.deleteTerm(termId);
        return ResponseEntity.noContent().build();
    }
}
