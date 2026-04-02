package com.exam.system.audit;

import com.exam.system.entity.ExamSession;
import com.exam.system.entity.ProctorAssign;
import com.exam.system.entity.RoomAssignment;
import com.exam.system.entity.RoomAssignmentId;
import com.exam.system.entity.SessionCandidate;
import com.exam.system.entity.SessionCandidateId;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.impl.VersionSnapshotRecorder;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class VersioningAspect {

    private final EntityManager entityManager;
    private final VersionSnapshotRecorder snapshotRecorder;

    public VersioningAspect(EntityManager entityManager, VersionSnapshotRecorder snapshotRecorder) {
        this.entityManager = entityManager;
        this.snapshotRecorder = snapshotRecorder;
    }

    @Around("execution(* com.exam.system.repository..*.save(..)) && args(entity)")
    public Object snapshotBeforeUpdate(ProceedingJoinPoint joinPoint, Object entity) throws Throwable {
        Object existing = existing(entity);
        if (existing != null && snapshotRecorder.isVersionedEntity(existing)) {
            UserContext context = UserContextHolder.get();
            Long changedBy = context != null ? context.getUserId() : null;
            snapshotRecorder.snapshot(existing, changedBy);
        }
        return joinPoint.proceed();
    }

    private Object existing(Object entity) {
        if (entity instanceof ExamSession session && session.getId() != null) {
            return entityManager.find(ExamSession.class, session.getId());
        }
        if (entity instanceof SessionCandidate candidate
                && candidate.getSessionId() != null
                && candidate.getStudentId() != null) {
            return entityManager.find(SessionCandidate.class,
                    new SessionCandidateId(candidate.getSessionId(), candidate.getStudentId()));
        }
        if (entity instanceof ProctorAssign assign && assign.getId() != null) {
            return entityManager.find(ProctorAssign.class, assign.getId());
        }
        if (entity instanceof RoomAssignment assignment
                && assignment.getSessionId() != null
                && assignment.getRoomId() != null) {
            return entityManager.find(RoomAssignment.class,
                    new RoomAssignmentId(assignment.getSessionId(), assignment.getRoomId()));
        }
        return null;
    }
}
