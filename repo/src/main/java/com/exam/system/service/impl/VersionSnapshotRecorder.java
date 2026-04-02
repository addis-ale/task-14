package com.exam.system.service.impl;

import com.exam.system.entity.EntityVersion;
import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ProctorAssign;
import com.exam.system.entity.RoomAssignment;
import com.exam.system.entity.SessionCandidate;
import com.exam.system.repository.EntityVersionRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class VersionSnapshotRecorder {

    public static final String EXAM_SESSION = "EXAM_SESSION";
    public static final String SESSION_CANDIDATE = "SESSION_CANDIDATE";
    public static final String PROCTOR_ASSIGN = "PROCTOR_ASSIGN";
    public static final String ROOM_ASSIGNMENT = "ROOM_ASSIGNMENT";

    private final EntityVersionRepository versionRepository;
    private final ExamSessionRepository examSessionRepository;
    private final ObjectMapper objectMapper;

    public VersionSnapshotRecorder(EntityVersionRepository versionRepository,
                                   ExamSessionRepository examSessionRepository,
                                   ObjectMapper objectMapper) {
        this.versionRepository = versionRepository;
        this.examSessionRepository = examSessionRepository;
        this.objectMapper = objectMapper;
    }

    public boolean isVersionedEntity(Object entity) {
        return entity instanceof ExamSession
                || entity instanceof SessionCandidate
                || entity instanceof ProctorAssign
                || entity instanceof RoomAssignment;
    }

    public void snapshot(Object entity, Long changedBy) {
        if (!isVersionedEntity(entity)) {
            return;
        }
        String entityType = entityType(entity);
        Long entityId = entityId(entity);
        Long termId = termId(entity);
        if (entityId == null) {
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(entity);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize version snapshot", ex);
        }

        EntityVersion latest = versionRepository.findTopByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId);
        int nextVersion = latest == null ? 1 : latest.getVersionNumber() + 1;

        EntityVersion version = new EntityVersion();
        version.setEntityType(entityType);
        version.setEntityId(entityId);
        version.setTermId(termId);
        version.setVersionNumber(nextVersion);
        version.setSnapshotJson(payload);
        version.setChangedBy(changedBy);
        versionRepository.save(version);
    }

    public String entityType(Object entity) {
        if (entity instanceof ExamSession) {
            return EXAM_SESSION;
        }
        if (entity instanceof SessionCandidate) {
            return SESSION_CANDIDATE;
        }
        if (entity instanceof ProctorAssign) {
            return PROCTOR_ASSIGN;
        }
        if (entity instanceof RoomAssignment) {
            return ROOM_ASSIGNMENT;
        }
        throw new IllegalArgumentException("Unsupported versioned entity");
    }

    public Long entityId(Object entity) {
        if (entity instanceof ExamSession session) {
            return session.getId();
        }
        if (entity instanceof SessionCandidate candidate) {
            return combine(candidate.getSessionId(), candidate.getStudentId());
        }
        if (entity instanceof ProctorAssign proctorAssign) {
            return proctorAssign.getId();
        }
        if (entity instanceof RoomAssignment assignment) {
            return combine(assignment.getSessionId(), assignment.getRoomId());
        }
        return null;
    }

    public Long termId(Object entity) {
        if (entity instanceof ExamSession session) {
            return session.getTermId();
        }
        if (entity instanceof SessionCandidate candidate) {
            return examSessionRepository.findById(candidate.getSessionId()).map(ExamSession::getTermId).orElse(null);
        }
        if (entity instanceof ProctorAssign assignment) {
            return examSessionRepository.findById(assignment.getSessionId()).map(ExamSession::getTermId).orElse(null);
        }
        if (entity instanceof RoomAssignment assignment) {
            return examSessionRepository.findById(assignment.getSessionId()).map(ExamSession::getTermId).orElse(null);
        }
        return null;
    }

    private Long combine(Long left, Long right) {
        if (left == null || right == null) {
            return null;
        }
        return (left << 32) ^ (right & 0xffffffffL);
    }
}
