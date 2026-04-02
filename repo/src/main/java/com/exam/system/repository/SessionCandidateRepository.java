package com.exam.system.repository;

import com.exam.system.entity.SessionCandidate;
import com.exam.system.entity.SessionCandidateId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionCandidateRepository extends JpaRepository<SessionCandidate, SessionCandidateId> {

    Page<SessionCandidate> findBySessionId(Long sessionId, Pageable pageable);

    List<SessionCandidate> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    long countBySessionIdAndRoomId(Long sessionId, Long roomId);

    long countByRoomId(Long roomId);

    @Query("""
            select sc.studentId as studentId, es.id as sessionId
            from SessionCandidate sc, ExamSession es
            where sc.sessionId = es.id
              and sc.studentId in :studentIds
              and es.date = :date
              and es.startTime < :endTime
              and es.endTime > :startTime
              and (:excludeSessionId is null or es.id <> :excludeSessionId)
            """)
    List<StudentConflictProjection> findStudentConflicts(@Param("studentIds") List<Long> studentIds,
                                                         @Param("date") LocalDate date,
                                                         @Param("startTime") LocalTime startTime,
                                                         @Param("endTime") LocalTime endTime,
                                                         @Param("excludeSessionId") Long excludeSessionId);

    @Query("""
            select es.id as sessionId,
                   s.name as subjectName,
                   es.date as examDate,
                   es.startTime as startTime,
                   es.endTime as endTime,
                   er.name as roomName,
                   c.name as campusName,
                   sc.seatNumber as seatNumber
            from SessionCandidate sc, ExamSession es, Subject s, ExamRoom er, Campus c
            where sc.sessionId = es.id
              and es.subjectId = s.id
              and sc.roomId = er.id
              and er.campusId = c.id
              and sc.studentId = :studentId
              and (:termId is null or es.termId = :termId)
              and (es.date > :today or (es.date = :today and es.endTime >= :nowTime))
            order by es.date asc, es.startTime asc
            """)
    List<StudentExamProjection> findUpcomingStudentExams(@Param("studentId") Long studentId,
                                                         @Param("termId") Long termId,
                                                         @Param("today") LocalDate today,
                                                         @Param("nowTime") LocalTime nowTime);
}
