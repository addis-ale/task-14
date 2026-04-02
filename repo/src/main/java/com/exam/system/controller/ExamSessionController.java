package com.exam.system.controller;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.session.CandidateSeatUpdateRequest;
import com.exam.system.dto.session.ConflictCheckRequest;
import com.exam.system.dto.session.ConflictReportResponse;
import com.exam.system.dto.session.ProctorScheduleItemResponse;
import com.exam.system.dto.session.SessionCandidateRequest;
import com.exam.system.dto.session.SessionCandidateResponse;
import com.exam.system.dto.session.SessionDetailResponse;
import com.exam.system.dto.session.SessionProctorRequest;
import com.exam.system.dto.session.SessionProctorResponse;
import com.exam.system.dto.session.SessionSummaryResponse;
import com.exam.system.dto.session.SessionUpsertRequest;
import com.exam.system.dto.session.StudentExamResponse;
import com.exam.system.security.rbac.ActiveRoleGuard;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.ExamSessionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
public class ExamSessionController {

    private final ExamSessionService examSessionService;
    private final ActiveRoleGuard activeRoleGuard;

    public ExamSessionController(ExamSessionService examSessionService, ActiveRoleGuard activeRoleGuard) {
        this.examSessionService = examSessionService;
        this.activeRoleGuard = activeRoleGuard;
    }

    @GetMapping("/api/v1/sessions")
    public ResponseEntity<ApiResponse<PageData<SessionSummaryResponse>>> listSessions(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.listSessions(termId, gradeId, subjectId, date, page, size)));
    }

    @GetMapping("/api/v1/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> getSession(@PathVariable Long sessionId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.getSession(sessionId)));
    }

    @PostMapping("/api/v1/sessions")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> createSession(@Valid @RequestBody SessionUpsertRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(examSessionService.createSession(request)));
    }

    @PutMapping("/api/v1/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionDetailResponse>> updateSession(@PathVariable Long sessionId,
                                                                            @Valid @RequestBody SessionUpsertRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.updateSession(sessionId, request)));
    }

    @DeleteMapping("/api/v1/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        examSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/sessions/conflict-check")
    public ResponseEntity<ApiResponse<ConflictReportResponse>> conflictCheck(@Valid @RequestBody ConflictCheckRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.conflictCheck(request)));
    }

    @GetMapping("/api/v1/sessions/{sessionId}/candidates")
    public ResponseEntity<ApiResponse<PageData<SessionCandidateResponse>>> listCandidates(@PathVariable Long sessionId,
                                                                                           @RequestParam(defaultValue = "1") int page,
                                                                                           @RequestParam(defaultValue = "20") int size) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.listCandidates(sessionId, page, size)));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/candidates")
    public ResponseEntity<ApiResponse<Void>> addCandidates(@PathVariable Long sessionId,
                                                           @Valid @RequestBody List<SessionCandidateRequest> candidates) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        examSessionService.addCandidates(sessionId, candidates);
        return ResponseEntity.ok(ApiResponse.successMessage("Candidates added"));
    }

    @DeleteMapping("/api/v1/sessions/{sessionId}/candidates/{studentId}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long sessionId, @PathVariable Long studentId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        examSessionService.deleteCandidate(sessionId, studentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/sessions/{sessionId}/candidates/{studentId}/seat")
    public ResponseEntity<ApiResponse<SessionCandidateResponse>> updateCandidateSeat(@PathVariable Long sessionId,
                                                                                     @PathVariable Long studentId,
                                                                                     @Valid @RequestBody CandidateSeatUpdateRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.updateCandidateSeat(sessionId, studentId, request)));
    }

    @GetMapping("/api/v1/sessions/{sessionId}/proctors")
    public ResponseEntity<ApiResponse<List<SessionProctorResponse>>> listProctors(@PathVariable Long sessionId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.listProctors(sessionId)));
    }

    @PostMapping("/api/v1/sessions/{sessionId}/proctors")
    public ResponseEntity<ApiResponse<SessionProctorResponse>> addProctor(@PathVariable Long sessionId,
                                                                          @Valid @RequestBody SessionProctorRequest request) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        return ResponseEntity.status(201).body(ApiResponse.success(examSessionService.addProctor(sessionId, request)));
    }

    @DeleteMapping("/api/v1/sessions/{sessionId}/proctors/{assignmentId}")
    public ResponseEntity<Void> deleteProctor(@PathVariable Long sessionId, @PathVariable Long assignmentId) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS");
        examSessionService.deleteProctor(sessionId, assignmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/proctors/{userId}/schedule")
    public ResponseEntity<ApiResponse<List<ProctorScheduleItemResponse>>> getProctorSchedule(
            @PathVariable Long userId,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) LocalDate date) {
        activeRoleGuard.requireAny("ADMIN", "ACADEMIC_AFFAIRS", "HOMEROOM_TEACHER", "SUBJECT_TEACHER");
        return ResponseEntity.ok(ApiResponse.success(examSessionService.getProctorSchedule(userId, termId, date)));
    }

    @GetMapping("/api/v1/my/exams")
    public ResponseEntity<ApiResponse<List<StudentExamResponse>>> myExams(@RequestParam(required = false) Long termId) {
        activeRoleGuard.requireAny("STUDENT");
        Long userId = UserContextHolder.get().getUserId();
        return ResponseEntity.ok(ApiResponse.success(examSessionService.getStudentExams(userId, termId)));
    }
}
