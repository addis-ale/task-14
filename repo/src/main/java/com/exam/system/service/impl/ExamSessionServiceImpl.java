package com.exam.system.service.impl;

import com.exam.system.dto.ScopeDto;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.session.CandidateSeatUpdateRequest;
import com.exam.system.dto.session.ConflictCheckRequest;
import com.exam.system.dto.session.ConflictItemResponse;
import com.exam.system.dto.session.ConflictReportResponse;
import com.exam.system.dto.session.ProctorScheduleItemResponse;
import com.exam.system.dto.session.SessionCandidateRequest;
import com.exam.system.dto.session.SessionCandidateResponse;
import com.exam.system.dto.session.SessionDetailResponse;
import com.exam.system.dto.session.SessionProctorRequest;
import com.exam.system.dto.session.SessionProctorResponse;
import com.exam.system.dto.session.SessionRoomAssignmentRequest;
import com.exam.system.dto.session.SessionRoomAssignmentResponse;
import com.exam.system.dto.session.SessionSummaryResponse;
import com.exam.system.dto.session.SessionUpsertRequest;
import com.exam.system.dto.session.StudentExamResponse;
import com.exam.system.entity.ExamRoom;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ProctorAssign;
import com.exam.system.entity.RoomAssignment;
import com.exam.system.entity.RoomAssignmentId;
import com.exam.system.entity.SessionCandidate;
import com.exam.system.entity.SessionCandidateId;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ConflictException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AcademicTermRepository;
import com.exam.system.repository.ExamRoomRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.ProctorAssignRepository;
import com.exam.system.repository.ProctorConflictProjection;
import com.exam.system.repository.RoomAssignmentRepository;
import com.exam.system.repository.RoomConflictProjection;
import com.exam.system.repository.SessionCandidateRepository;
import com.exam.system.repository.StudentConflictProjection;
import com.exam.system.repository.StudentExamProjection;
import com.exam.system.repository.SubjectRepository;
import com.exam.system.security.rbac.SessionDataScopeGuard;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.ExamSessionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExamSessionServiceImpl implements ExamSessionService {

    private final ExamSessionRepository examSessionRepository;
    private final SessionCandidateRepository sessionCandidateRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final ProctorAssignRepository proctorAssignRepository;
    private final ExamRoomRepository examRoomRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicTermRepository academicTermRepository;
    private final PageDataBuilder pageDataBuilder;
    private final SessionDataScopeGuard sessionDataScopeGuard;

    public ExamSessionServiceImpl(ExamSessionRepository examSessionRepository,
                                  SessionCandidateRepository sessionCandidateRepository,
                                  RoomAssignmentRepository roomAssignmentRepository,
                                  ProctorAssignRepository proctorAssignRepository,
                                  ExamRoomRepository examRoomRepository,
                                  SubjectRepository subjectRepository,
                                  AcademicTermRepository academicTermRepository,
                                  PageDataBuilder pageDataBuilder,
                                  SessionDataScopeGuard sessionDataScopeGuard) {
        this.examSessionRepository = examSessionRepository;
        this.sessionCandidateRepository = sessionCandidateRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.proctorAssignRepository = proctorAssignRepository;
        this.examRoomRepository = examRoomRepository;
        this.subjectRepository = subjectRepository;
        this.academicTermRepository = academicTermRepository;
        this.pageDataBuilder = pageDataBuilder;
        this.sessionDataScopeGuard = sessionDataScopeGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<SessionSummaryResponse> listSessions(Long termId, Long gradeId, Long subjectId, LocalDate date, int page, int size) {
        Specification<ExamSession> spec = Specification.where(null);
        if (termId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("termId"), termId));
        }
        if (gradeId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("gradeId"), gradeId));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subjectId"), subjectId));
        }
        if (date != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("date"), date));
        }

        UserContext context = UserContextHolder.get();
        if (context != null && !isPrivileged(context)) {
            ScopeDto scope = context.getScope();
            if (scope != null && scope.getTermId() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("termId"), scope.getTermId()));
            }
            if (scope != null && !scope.getGradeIds().isEmpty()) {
                spec = spec.and((root, query, cb) -> root.get("gradeId").in(scope.getGradeIds()));
            }
        }

        Page<ExamSession> sessions = examSessionRepository.findAll(spec,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return pageDataBuilder.from(sessions, this::toSummary, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionDetailResponse getSession(Long sessionId) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        return buildDetail(session);
    }

    @Override
    @Transactional
    public SessionDetailResponse createSession(SessionUpsertRequest request) {
        validateSessionRequest(request);
        ensureCreateWithinScope(request);

        ConflictReportResponse conflicts = checkConflicts(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCandidates(),
                request.getRoomAssignments(),
                request.getProctors(),
                null);
        if (conflicts.hasConflict()) {
            throw new ConflictException("Scheduling conflicts detected", conflicts);
        }

        ExamSession session = new ExamSession();
        applyUpsert(session, request);
        session.setCreatedBy(currentUserId());
        ExamSession saved = examSessionRepository.save(session);
        persistChildren(saved.getId(), request);
        return buildDetail(saved);
    }

    @Override
    @Transactional
    public SessionDetailResponse updateSession(Long sessionId, SessionUpsertRequest request) {
        validateSessionRequest(request);
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);

        ConflictReportResponse conflicts = checkConflicts(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCandidates(),
                request.getRoomAssignments(),
                request.getProctors(),
                sessionId);
        if (conflicts.hasConflict()) {
            throw new ConflictException("Scheduling conflicts detected", conflicts);
        }

        applyUpsert(session, request);
        ExamSession saved = examSessionRepository.save(session);

        proctorAssignRepository.deleteBySessionId(sessionId);
        sessionCandidateRepository.deleteBySessionId(sessionId);
        roomAssignmentRepository.deleteBySessionId(sessionId);
        persistChildren(sessionId, request);
        return buildDetail(saved);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        proctorAssignRepository.deleteBySessionId(sessionId);
        sessionCandidateRepository.deleteBySessionId(sessionId);
        roomAssignmentRepository.deleteBySessionId(sessionId);
        examSessionRepository.delete(session);
    }

    @Override
    @Transactional(readOnly = true)
    public ConflictReportResponse conflictCheck(ConflictCheckRequest request) {
        return checkConflicts(
                request.getDate(),
                request.getStartTime(),
                request.getEndTime(),
                request.getCandidates(),
                request.getRoomAssignments(),
                request.getProctors(),
                request.getExcludeSessionId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<SessionCandidateResponse> listCandidates(Long sessionId, int page, int size) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        Page<SessionCandidate> candidates = sessionCandidateRepository.findBySessionId(sessionId,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return pageDataBuilder.from(candidates, this::toCandidateResponse, page, size);
    }

    @Override
    @Transactional
    public void addCandidates(Long sessionId, List<SessionCandidateRequest> candidates) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        ConflictReportResponse conflicts = checkConflicts(
                session.getDate(),
                session.getStartTime(),
                session.getEndTime(),
                candidates,
                toRoomRequestList(roomAssignmentRepository.findBySessionId(sessionId)),
                List.of(),
                sessionId);
        if (conflicts.hasConflict()) {
            throw new ConflictException("Candidate conflicts detected", conflicts);
        }

        Map<Long, RoomAssignment> roomAssignmentMap = roomAssignmentRepository.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(RoomAssignment::getRoomId, r -> r));

        for (SessionCandidateRequest request : candidates) {
            SessionCandidateId id = new SessionCandidateId(sessionId, request.getStudentId());
            if (sessionCandidateRepository.existsById(id)) {
                throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                        "Candidate already exists in session");
            }

            ExamRoom room = examRoomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Room not found"));

            RoomAssignment assignment = roomAssignmentMap.get(request.getRoomId());
            if (assignment == null) {
                assignment = new RoomAssignment();
                assignment.setSessionId(sessionId);
                assignment.setRoomId(request.getRoomId());
                assignment.setAssignedCount(0);
            }
            if (assignment.getAssignedCount() + 1 > room.getCapacity()) {
                ConflictReportResponse report = new ConflictReportResponse();
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(room.getId());
                item.setExistingSessionId(sessionId);
                item.setConflictType("ROOM_CAPACITY_EXCEEDED");
                report.setConflicts(List.of(item));
                throw new ConflictException("Room capacity exceeded", report);
            }

            assignment.setAssignedCount(assignment.getAssignedCount() + 1);
            roomAssignmentRepository.save(assignment);
            roomAssignmentMap.put(assignment.getRoomId(), assignment);

            SessionCandidate candidate = new SessionCandidate();
            candidate.setSessionId(sessionId);
            candidate.setStudentId(request.getStudentId());
            candidate.setSeatNumber(request.getSeatNumber());
            candidate.setRoomId(request.getRoomId());
            sessionCandidateRepository.save(candidate);
        }
    }

    @Override
    @Transactional
    public void deleteCandidate(Long sessionId, Long studentId) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);

        SessionCandidateId id = new SessionCandidateId(sessionId, studentId);
        SessionCandidate candidate = sessionCandidateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Candidate not found"));

        if (candidate.getRoomId() != null) {
            roomAssignmentRepository.findById(new RoomAssignmentId(sessionId, candidate.getRoomId()))
                    .ifPresent(assignment -> {
                        assignment.setAssignedCount(Math.max(0, assignment.getAssignedCount() - 1));
                        roomAssignmentRepository.save(assignment);
                    });
        }
        sessionCandidateRepository.delete(candidate);
    }

    @Override
    @Transactional
    public SessionCandidateResponse updateCandidateSeat(Long sessionId, Long studentId, CandidateSeatUpdateRequest request) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);

        SessionCandidateId id = new SessionCandidateId(sessionId, studentId);
        SessionCandidate candidate = sessionCandidateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Candidate not found"));

        Long oldRoomId = candidate.getRoomId();
        Long newRoomId = request.getRoomId();
        if (!Objects.equals(oldRoomId, newRoomId)) {
            ExamRoom targetRoom = examRoomRepository.findById(newRoomId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Target room not found"));

            if (oldRoomId != null) {
                roomAssignmentRepository.findById(new RoomAssignmentId(sessionId, oldRoomId)).ifPresent(assignment -> {
                    assignment.setAssignedCount(Math.max(0, assignment.getAssignedCount() - 1));
                    roomAssignmentRepository.save(assignment);
                });
            }

            RoomAssignment targetAssignment = roomAssignmentRepository.findById(new RoomAssignmentId(sessionId, newRoomId))
                    .orElseGet(() -> {
                        RoomAssignment roomAssignment = new RoomAssignment();
                        roomAssignment.setSessionId(sessionId);
                        roomAssignment.setRoomId(newRoomId);
                        roomAssignment.setAssignedCount(0);
                        return roomAssignment;
                    });

            if (targetAssignment.getAssignedCount() + 1 > targetRoom.getCapacity()) {
                throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Target room capacity exceeded");
            }
            targetAssignment.setAssignedCount(targetAssignment.getAssignedCount() + 1);
            roomAssignmentRepository.save(targetAssignment);
            candidate.setRoomId(newRoomId);
        }

        candidate.setSeatNumber(request.getSeatNumber());
        SessionCandidate saved = sessionCandidateRepository.save(candidate);
        return toCandidateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionProctorResponse> listProctors(Long sessionId) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        return proctorAssignRepository.findBySessionId(sessionId).stream()
                .sorted(Comparator.comparing(ProctorAssign::getTimeSlotStart))
                .map(this::toProctorResponse)
                .toList();
    }

    @Override
    @Transactional
    public SessionProctorResponse addProctor(Long sessionId, SessionProctorRequest request) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        examRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Room not found"));

        List<ProctorConflictProjection> conflicts = proctorAssignRepository.findOverlappingAssignments(
                List.of(request.getProctorUserId()), request.getTimeSlotStart(), request.getTimeSlotEnd(), null);
        if (!conflicts.isEmpty()) {
            ConflictReportResponse report = new ConflictReportResponse();
            List<ConflictItemResponse> items = conflicts.stream().map(conflict -> {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(conflict.getProctorUserId());
                item.setExistingSessionId(conflict.getSessionId());
                item.setConflictType("PROCTOR_TIME_CONFLICT");
                return item;
            }).toList();
            report.setConflicts(items);
            throw new ConflictException("Proctor scheduling conflict detected", report);
        }

        ProctorAssign assign = new ProctorAssign();
        assign.setSessionId(sessionId);
        assign.setProctorUserId(request.getProctorUserId());
        assign.setRoomId(request.getRoomId());
        assign.setTimeSlotStart(request.getTimeSlotStart());
        assign.setTimeSlotEnd(request.getTimeSlotEnd());
        return toProctorResponse(proctorAssignRepository.save(assign));
    }

    @Override
    @Transactional
    public void deleteProctor(Long sessionId, Long assignmentId) {
        ExamSession session = findSessionOrThrow(sessionId);
        sessionDataScopeGuard.ensureAccessible(session);
        ProctorAssign assignment = proctorAssignRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Proctor assignment not found"));
        if (!assignment.getSessionId().equals(sessionId)) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Assignment does not belong to session");
        }
        proctorAssignRepository.delete(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProctorScheduleItemResponse> getProctorSchedule(Long proctorUserId, Long termId, LocalDate date) {
        LocalDateTime start = date == null ? null : date.atStartOfDay();
        LocalDateTime end = date == null ? null : date.plusDays(1).atStartOfDay();
        List<ProctorAssign> assignments = proctorAssignRepository.findUserSchedule(proctorUserId, start, end);

        Map<Long, ExamSession> sessions = examSessionRepository.findAllById(
                assignments.stream().map(ProctorAssign::getSessionId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s));

        return assignments.stream()
                .filter(a -> termId == null || (sessions.containsKey(a.getSessionId())
                        && termId.equals(sessions.get(a.getSessionId()).getTermId())))
                .map(a -> {
                    ProctorScheduleItemResponse item = new ProctorScheduleItemResponse();
                    item.setAssignmentId(a.getId());
                    item.setSessionId(a.getSessionId());
                    item.setRoomId(a.getRoomId());
                    item.setTimeSlotStart(a.getTimeSlotStart());
                    item.setTimeSlotEnd(a.getTimeSlotEnd());
                    return item;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentExamResponse> getStudentExams(Long studentId, Long termId) {
        List<StudentExamProjection> projections = sessionCandidateRepository.findUpcomingStudentExams(
                studentId, termId, LocalDate.now(), LocalTime.now());
        return projections.stream().map(p -> {
            StudentExamResponse response = new StudentExamResponse();
            response.setSessionId(p.getSessionId());
            response.setSubject(p.getSubjectName());
            response.setDate(p.getExamDate());
            response.setStartTime(p.getStartTime());
            response.setEndTime(p.getEndTime());
            response.setRoom(p.getRoomName());
            response.setCampus(p.getCampusName());
            response.setSeat(p.getSeatNumber());
            return response;
        }).toList();
    }

    @Override
    @Transactional
    public void publishSession(Long sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Session not found"));
        session.setStatus("PUBLISHED");
        examSessionRepository.save(session);
    }

    private void validateSessionRequest(SessionUpsertRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Session start time must be before end time");
        }
        if (!academicTermRepository.existsById(request.getTermId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Invalid termId");
        }
        if (!subjectRepository.existsById(request.getSubjectId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Invalid subjectId");
        }
    }

    private void ensureCreateWithinScope(SessionUpsertRequest request) {
        UserContext context = UserContextHolder.get();
        if (context == null || isPrivileged(context)) {
            return;
        }
        ScopeDto scope = context.getScope();
        if (scope == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Out of data scope");
        }
        if (scope.getTermId() != null && !scope.getTermId().equals(request.getTermId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Out of term scope");
        }
        if (!scope.getGradeIds().isEmpty() && !scope.getGradeIds().contains(request.getGradeId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Out of grade scope");
        }
    }

    private ConflictReportResponse checkConflicts(LocalDate date,
                                                  LocalTime startTime,
                                                  LocalTime endTime,
                                                  List<SessionCandidateRequest> candidates,
                                                  List<SessionRoomAssignmentRequest> roomAssignments,
                                                  List<SessionProctorRequest> proctors,
                                                  Long excludeSessionId) {
        ConflictReportResponse report = new ConflictReportResponse();
        List<ConflictItemResponse> items = new ArrayList<>();

        List<Long> studentIds = candidates == null ? List.of() : candidates.stream()
                .map(SessionCandidateRequest::getStudentId)
                .distinct()
                .toList();
        if (!studentIds.isEmpty()) {
            List<StudentConflictProjection> studentConflicts = sessionCandidateRepository.findStudentConflicts(
                    studentIds, date, startTime, endTime, excludeSessionId);
            studentConflicts.forEach(conflict -> {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(conflict.getStudentId());
                item.setExistingSessionId(conflict.getSessionId());
                item.setConflictType("STUDENT_TIME_CONFLICT");
                items.add(item);
            });
        }

        List<Long> roomIds = roomAssignments == null ? List.of() : roomAssignments.stream()
                .map(SessionRoomAssignmentRequest::getRoomId)
                .distinct()
                .toList();
        if (!roomIds.isEmpty()) {
            List<RoomConflictProjection> roomConflicts = roomAssignmentRepository.findRoomConflicts(
                    roomIds, date, startTime, endTime, excludeSessionId);
            roomConflicts.forEach(conflict -> {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(conflict.getRoomId());
                item.setExistingSessionId(conflict.getSessionId());
                item.setConflictType("ROOM_TIME_CONFLICT");
                items.add(item);
            });
        }

        if (proctors != null && !proctors.isEmpty()) {
            for (SessionProctorRequest request : proctors) {
                List<ProctorConflictProjection> proctorConflicts = proctorAssignRepository.findOverlappingAssignments(
                        List.of(request.getProctorUserId()), request.getTimeSlotStart(), request.getTimeSlotEnd(), excludeSessionId);
                proctorConflicts.forEach(conflict -> {
                    ConflictItemResponse item = new ConflictItemResponse();
                    item.setTargetId(conflict.getProctorUserId());
                    item.setExistingSessionId(conflict.getSessionId());
                    item.setConflictType("PROCTOR_TIME_CONFLICT");
                    items.add(item);
                });
            }
            items.addAll(internalProctorConflicts(proctors));
        }

        items.addAll(capacityConflicts(candidates, roomAssignments));

        report.setConflicts(items.stream().distinct().toList());
        return report;
    }

    private List<ConflictItemResponse> internalProctorConflicts(List<SessionProctorRequest> proctors) {
        List<ConflictItemResponse> conflicts = new ArrayList<>();
        for (int i = 0; i < proctors.size(); i++) {
            for (int j = i + 1; j < proctors.size(); j++) {
                SessionProctorRequest left = proctors.get(i);
                SessionProctorRequest right = proctors.get(j);
                if (left.getProctorUserId().equals(right.getProctorUserId())
                        && left.getTimeSlotStart().isBefore(right.getTimeSlotEnd())
                        && left.getTimeSlotEnd().isAfter(right.getTimeSlotStart())
                        && !left.getRoomId().equals(right.getRoomId())) {
                    ConflictItemResponse item = new ConflictItemResponse();
                    item.setTargetId(left.getProctorUserId());
                    item.setConflictType("PROCTOR_TIME_CONFLICT");
                    conflicts.add(item);
                }
            }
        }
        return conflicts;
    }

    private List<ConflictItemResponse> capacityConflicts(List<SessionCandidateRequest> candidates,
                                                         List<SessionRoomAssignmentRequest> roomAssignments) {
        List<ConflictItemResponse> conflicts = new ArrayList<>();
        Map<Long, Integer> declaredAssignments = new HashMap<>();
        if (roomAssignments != null) {
            for (SessionRoomAssignmentRequest assignment : roomAssignments) {
                declaredAssignments.put(assignment.getRoomId(), assignment.getAssignedCount());
            }
        }

        Map<Long, Long> candidateCountByRoom = candidates == null ? Map.of() : candidates.stream()
                .collect(Collectors.groupingBy(SessionCandidateRequest::getRoomId, Collectors.counting()));

        Set<Long> roomIds = new HashSet<>();
        roomIds.addAll(declaredAssignments.keySet());
        roomIds.addAll(candidateCountByRoom.keySet());
        if (roomIds.isEmpty()) {
            return conflicts;
        }

        Map<Long, ExamRoom> rooms = examRoomRepository.findByIdIn(new ArrayList<>(roomIds)).stream()
                .collect(Collectors.toMap(ExamRoom::getId, r -> r));

        for (Long roomId : roomIds) {
            ExamRoom room = rooms.get(roomId);
            if (room == null) {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(roomId);
                item.setConflictType("ROOM_NOT_FOUND");
                conflicts.add(item);
                continue;
            }

            int declared = declaredAssignments.getOrDefault(roomId, 0);
            int candidateCount = candidateCountByRoom.getOrDefault(roomId, 0L).intValue();

            if (declared > room.getCapacity() || candidateCount > room.getCapacity()) {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(roomId);
                item.setConflictType("ROOM_CAPACITY_EXCEEDED");
                conflicts.add(item);
            }
            if (declared > 0 && candidateCount > declared) {
                ConflictItemResponse item = new ConflictItemResponse();
                item.setTargetId(roomId);
                item.setConflictType("ASSIGNED_COUNT_EXCEEDED");
                conflicts.add(item);
            }
        }
        return conflicts;
    }

    private void persistChildren(Long sessionId, SessionUpsertRequest request) {
        Map<Long, Integer> declared = request.getRoomAssignments().stream()
                .collect(Collectors.toMap(SessionRoomAssignmentRequest::getRoomId,
                        SessionRoomAssignmentRequest::getAssignedCount,
                        Integer::sum));
        Map<Long, Long> candidateCount = request.getCandidates().stream()
                .collect(Collectors.groupingBy(SessionCandidateRequest::getRoomId, Collectors.counting()));

        Set<Long> roomIds = new HashSet<>();
        roomIds.addAll(declared.keySet());
        roomIds.addAll(candidateCount.keySet());
        Map<Long, ExamRoom> rooms = examRoomRepository.findByIdIn(new ArrayList<>(roomIds)).stream()
                .collect(Collectors.toMap(ExamRoom::getId, r -> r));

        for (Long roomId : roomIds) {
            int assignedCount = declared.getOrDefault(roomId, candidateCount.getOrDefault(roomId, 0L).intValue());
            int candidatesInRoom = candidateCount.getOrDefault(roomId, 0L).intValue();
            if (candidatesInRoom > assignedCount) {
                assignedCount = candidatesInRoom;
            }
            ExamRoom room = rooms.get(roomId);
            if (room == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Room not found");
            }
            if (assignedCount > room.getCapacity()) {
                throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Room capacity exceeded");
            }

            RoomAssignment assignment = new RoomAssignment();
            assignment.setSessionId(sessionId);
            assignment.setRoomId(roomId);
            assignment.setAssignedCount(assignedCount);
            roomAssignmentRepository.save(assignment);
        }

        for (SessionCandidateRequest candidateRequest : request.getCandidates()) {
            SessionCandidate candidate = new SessionCandidate();
            candidate.setSessionId(sessionId);
            candidate.setStudentId(candidateRequest.getStudentId());
            candidate.setRoomId(candidateRequest.getRoomId());
            candidate.setSeatNumber(candidateRequest.getSeatNumber());
            sessionCandidateRepository.save(candidate);
        }

        for (SessionProctorRequest proctorRequest : request.getProctors()) {
            ProctorAssign proctorAssign = new ProctorAssign();
            proctorAssign.setSessionId(sessionId);
            proctorAssign.setRoomId(proctorRequest.getRoomId());
            proctorAssign.setProctorUserId(proctorRequest.getProctorUserId());
            proctorAssign.setTimeSlotStart(proctorRequest.getTimeSlotStart());
            proctorAssign.setTimeSlotEnd(proctorRequest.getTimeSlotEnd());
            proctorAssignRepository.save(proctorAssign);
        }
    }

    private SessionDetailResponse buildDetail(ExamSession session) {
        SessionDetailResponse detail = new SessionDetailResponse();
        detail.setId(session.getId());
        detail.setTermId(session.getTermId());
        detail.setSubjectId(session.getSubjectId());
        detail.setGradeId(session.getGradeId());
        detail.setDate(session.getDate());
        detail.setStartTime(session.getStartTime());
        detail.setEndTime(session.getEndTime());
        detail.setStatus(session.getStatus());

        List<SessionCandidateResponse> candidates = sessionCandidateRepository.findBySessionId(session.getId())
                .stream()
                .map(this::toCandidateResponse)
                .toList();
        detail.setCandidates(candidates);

        List<RoomAssignment> roomAssignments = roomAssignmentRepository.findBySessionId(session.getId());
        Map<Long, ExamRoom> roomMap = examRoomRepository.findByIdIn(roomAssignments.stream()
                        .map(RoomAssignment::getRoomId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ExamRoom::getId, r -> r));

        List<SessionRoomAssignmentResponse> assignmentResponses = roomAssignments.stream().map(assignment -> {
            SessionRoomAssignmentResponse response = new SessionRoomAssignmentResponse();
            response.setRoomId(assignment.getRoomId());
            response.setAssignedCount(assignment.getAssignedCount());
            ExamRoom room = roomMap.get(assignment.getRoomId());
            if (room != null) {
                response.setCapacity(room.getCapacity());
                response.setRemainingCapacity(Math.max(0, room.getCapacity() - assignment.getAssignedCount()));
            } else {
                response.setCapacity(0);
                response.setRemainingCapacity(0);
            }
            return response;
        }).toList();
        detail.setRoomAssignments(assignmentResponses);

        List<SessionProctorResponse> proctors = proctorAssignRepository.findBySessionId(session.getId()).stream()
                .map(this::toProctorResponse)
                .toList();
        detail.setProctors(proctors);

        return detail;
    }

    private List<SessionRoomAssignmentRequest> toRoomRequestList(List<RoomAssignment> assignments) {
        return assignments.stream().map(assignment -> {
            SessionRoomAssignmentRequest request = new SessionRoomAssignmentRequest();
            request.setRoomId(assignment.getRoomId());
            request.setAssignedCount(assignment.getAssignedCount());
            return request;
        }).toList();
    }

    private ExamSession findSessionOrThrow(Long sessionId) {
        return examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Exam session not found"));
    }

    private void applyUpsert(ExamSession session, SessionUpsertRequest request) {
        session.setTermId(request.getTermId());
        session.setSubjectId(request.getSubjectId());
        session.setGradeId(request.getGradeId());
        session.setDate(request.getDate());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setStatus(request.getStatus());
    }

    private SessionSummaryResponse toSummary(ExamSession session) {
        SessionSummaryResponse response = new SessionSummaryResponse();
        response.setId(session.getId());
        response.setTermId(session.getTermId());
        response.setTermName(academicTermRepository.findById(session.getTermId())
                .map(t -> t.getName()).orElse("Term " + session.getTermId()));
        response.setSubjectId(session.getSubjectId());
        response.setSubjectName(subjectRepository.findById(session.getSubjectId())
                .map(s -> s.getName()).orElse("Subject " + session.getSubjectId()));
        response.setGradeId(session.getGradeId());
        response.setGradeName("Grade " + session.getGradeId());
        response.setDate(session.getDate());
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setStatus(session.getStatus());
        return response;
    }

    private SessionCandidateResponse toCandidateResponse(SessionCandidate candidate) {
        SessionCandidateResponse response = new SessionCandidateResponse();
        response.setStudentId(candidate.getStudentId());
        response.setMaskedStudentId(maskId(candidate.getStudentId()));
        response.setRoomId(candidate.getRoomId());
        response.setSeatNumber(candidate.getSeatNumber());
        return response;
    }

    private SessionProctorResponse toProctorResponse(ProctorAssign assign) {
        SessionProctorResponse response = new SessionProctorResponse();
        response.setId(assign.getId());
        response.setProctorUserId(assign.getProctorUserId());
        response.setRoomId(assign.getRoomId());
        response.setTimeSlotStart(assign.getTimeSlotStart());
        response.setTimeSlotEnd(assign.getTimeSlotEnd());
        return response;
    }

    private boolean isPrivileged(UserContext context) {
        return "ADMIN".equalsIgnoreCase(context.getActiveRole())
                || "ACADEMIC_AFFAIRS".equalsIgnoreCase(context.getActiveRole());
    }

    private Long currentUserId() {
        UserContext context = UserContextHolder.get();
        if (context == null) {
            throw new BusinessException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED, "User context required");
        }
        return context.getUserId();
    }

    private String maskId(Long studentId) {
        if (studentId == null) {
            return null;
        }
        String raw = String.valueOf(studentId);
        if (raw.length() <= 4) {
            return "****" + raw;
        }
        return "****" + raw.substring(raw.length() - 4);
    }
}
