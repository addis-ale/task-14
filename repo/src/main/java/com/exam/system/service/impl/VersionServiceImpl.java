package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.version.FieldDiffResponse;
import com.exam.system.dto.version.VersionCompareResponse;
import com.exam.system.dto.version.VersionDetailResponse;
import com.exam.system.dto.version.VersionSummaryResponse;
import com.exam.system.entity.EntityVersion;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ProctorAssign;
import com.exam.system.entity.RoomAssignment;
import com.exam.system.entity.SessionCandidate;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.EntityVersionRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.ProctorAssignRepository;
import com.exam.system.repository.RoomAssignmentRepository;
import com.exam.system.repository.SessionCandidateRepository;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.dto.ScopeDto;
import com.exam.system.service.VersionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VersionServiceImpl implements VersionService {

    private final EntityVersionRepository versionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final SessionCandidateRepository sessionCandidateRepository;
    private final ProctorAssignRepository proctorAssignRepository;
    private final RoomAssignmentRepository roomAssignmentRepository;
    private final PageDataBuilder pageDataBuilder;
    private final VersionSnapshotRecorder snapshotRecorder;
    private final ObjectMapper objectMapper;

    public VersionServiceImpl(EntityVersionRepository versionRepository,
                              ExamSessionRepository examSessionRepository,
                              SessionCandidateRepository sessionCandidateRepository,
                              ProctorAssignRepository proctorAssignRepository,
                              RoomAssignmentRepository roomAssignmentRepository,
                              PageDataBuilder pageDataBuilder,
                              VersionSnapshotRecorder snapshotRecorder,
                              ObjectMapper objectMapper) {
        this.versionRepository = versionRepository;
        this.examSessionRepository = examSessionRepository;
        this.sessionCandidateRepository = sessionCandidateRepository;
        this.proctorAssignRepository = proctorAssignRepository;
        this.roomAssignmentRepository = roomAssignmentRepository;
        this.pageDataBuilder = pageDataBuilder;
        this.snapshotRecorder = snapshotRecorder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<VersionSummaryResponse> listVersions(String entityType, Long entityId, int page, int size) {
        Page<EntityVersion> versions = versionRepository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                entityType.toUpperCase(), entityId, PageRequest.of(Math.max(0, page - 1), Math.min(100, size)));
        return pageDataBuilder.from(versions, version -> {
            ensureScope(version);
            return toSummary(version);
        }, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public VersionDetailResponse getVersion(Long versionId) {
        EntityVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Version not found"));
        ensureScope(version);
        VersionDetailResponse response = new VersionDetailResponse();
        fill(response, version);
        response.setSnapshotJson(version.getSnapshotJson());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public VersionCompareResponse compare(Long versionA, Long versionB) {
        EntityVersion left = versionRepository.findById(versionA)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Version A not found"));
        EntityVersion right = versionRepository.findById(versionB)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Version B not found"));
        ensureScope(left);
        ensureScope(right);

        Map<String, Object> mapA = parseJson(left.getSnapshotJson());
        Map<String, Object> mapB = parseJson(right.getSnapshotJson());

        Set<String> keys = new HashSet<>();
        keys.addAll(mapA.keySet());
        keys.addAll(mapB.keySet());

        VersionCompareResponse response = new VersionCompareResponse();
        response.setVersionA(versionA);
        response.setVersionB(versionB);

        ArrayList<FieldDiffResponse> diffs = new ArrayList<>();
        for (String key : keys) {
            String oldValue = String.valueOf(mapA.get(key));
            String newValue = String.valueOf(mapB.get(key));
            if (!oldValue.equals(newValue)) {
                FieldDiffResponse diff = new FieldDiffResponse();
                diff.setField(key);
                diff.setOldValue("null".equals(oldValue) ? null : oldValue);
                diff.setNewValue("null".equals(newValue) ? null : newValue);
                diffs.add(diff);
            }
        }
        response.setDiffs(diffs);
        return response;
    }

    @Override
    @Transactional
    public void restore(Long versionId) {
        EntityVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Version not found"));
        ensureScope(version);

        Long changedBy = UserContextHolder.get() != null ? UserContextHolder.get().getUserId() : null;

        try {
            switch (version.getEntityType()) {
                case VersionSnapshotRecorder.EXAM_SESSION -> {
                    ExamSession restored = objectMapper.readValue(version.getSnapshotJson(), ExamSession.class);
                    examSessionRepository.save(restored);
                    snapshotRecorder.snapshot(restored, changedBy);
                }
                case VersionSnapshotRecorder.SESSION_CANDIDATE -> {
                    SessionCandidate restored = objectMapper.readValue(version.getSnapshotJson(), SessionCandidate.class);
                    sessionCandidateRepository.save(restored);
                    snapshotRecorder.snapshot(restored, changedBy);
                }
                case VersionSnapshotRecorder.PROCTOR_ASSIGN -> {
                    ProctorAssign restored = objectMapper.readValue(version.getSnapshotJson(), ProctorAssign.class);
                    proctorAssignRepository.save(restored);
                    snapshotRecorder.snapshot(restored, changedBy);
                }
                case VersionSnapshotRecorder.ROOM_ASSIGNMENT -> {
                    RoomAssignment restored = objectMapper.readValue(version.getSnapshotJson(), RoomAssignment.class);
                    roomAssignmentRepository.save(restored);
                    snapshotRecorder.snapshot(restored, changedBy);
                }
                default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                        "Unsupported entity type for restore");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to restore version snapshot");
        }
    }

    private VersionSummaryResponse toSummary(EntityVersion version) {
        VersionSummaryResponse response = new VersionSummaryResponse();
        fill(response, version);
        return response;
    }

    private void fill(VersionSummaryResponse response, EntityVersion version) {
        response.setId(version.getId());
        response.setEntityType(version.getEntityType());
        response.setEntityId(version.getEntityId());
        response.setTermId(version.getTermId());
        response.setVersionNumber(version.getVersionNumber());
        response.setChangedBy(version.getChangedBy());
        response.setChangedAt(version.getChangedAt());
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to parse snapshot JSON");
        }
    }

    private void ensureScope(EntityVersion version) {
        if (UserContextHolder.get() == null) {
            return;
        }
        String activeRole = UserContextHolder.get().getActiveRole();
        if ("ADMIN".equalsIgnoreCase(activeRole) || "ACADEMIC_AFFAIRS".equalsIgnoreCase(activeRole)) {
            return;
        }
        ScopeDto scope = UserContextHolder.get().getScope();
        if (scope != null && scope.getTermId() != null && version.getTermId() != null
                && !scope.getTermId().equals(version.getTermId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Version out of data scope");
        }
    }
}
