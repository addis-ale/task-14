package com.exam.system.service;

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
import java.time.LocalDate;
import java.util.List;

public interface ExamSessionService {

    PageData<SessionSummaryResponse> listSessions(Long termId, Long gradeId, Long subjectId, LocalDate date, int page, int size);

    SessionDetailResponse getSession(Long sessionId);

    SessionDetailResponse createSession(SessionUpsertRequest request);

    SessionDetailResponse updateSession(Long sessionId, SessionUpsertRequest request);

    void deleteSession(Long sessionId);

    ConflictReportResponse conflictCheck(ConflictCheckRequest request);

    PageData<SessionCandidateResponse> listCandidates(Long sessionId, int page, int size);

    void addCandidates(Long sessionId, List<SessionCandidateRequest> candidates);

    void deleteCandidate(Long sessionId, Long studentId);

    SessionCandidateResponse updateCandidateSeat(Long sessionId, Long studentId, CandidateSeatUpdateRequest request);

    List<SessionProctorResponse> listProctors(Long sessionId);

    SessionProctorResponse addProctor(Long sessionId, SessionProctorRequest request);

    void deleteProctor(Long sessionId, Long assignmentId);

    List<ProctorScheduleItemResponse> getProctorSchedule(Long proctorUserId, Long termId, LocalDate date);

    List<StudentExamResponse> getStudentExams(Long studentId, Long termId);
}
