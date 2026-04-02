package com.exam.system.repository;

import com.exam.system.entity.ProctorAssign;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProctorAssignRepository extends JpaRepository<ProctorAssign, Long> {

    List<ProctorAssign> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    long countByRoomId(Long roomId);

    @Query("""
            select pa.proctorUserId as proctorUserId,
                   pa.sessionId as sessionId,
                   pa.id as assignmentId
            from ProctorAssign pa
            where pa.proctorUserId in :proctorUserIds
              and pa.timeSlotStart < :newEnd
              and pa.timeSlotEnd > :newStart
              and (:excludeSessionId is null or pa.sessionId <> :excludeSessionId)
            """)
    List<ProctorConflictProjection> findOverlappingAssignments(@Param("proctorUserIds") List<Long> proctorUserIds,
                                                               @Param("newStart") LocalDateTime newStart,
                                                               @Param("newEnd") LocalDateTime newEnd,
                                                               @Param("excludeSessionId") Long excludeSessionId);

    @Query("""
            select pa from ProctorAssign pa
            where pa.proctorUserId = :proctorUserId
              and (:startInclusive is null or pa.timeSlotStart >= :startInclusive)
              and (:endExclusive is null or pa.timeSlotStart < :endExclusive)
            order by pa.timeSlotStart asc
            """)
    List<ProctorAssign> findUserSchedule(@Param("proctorUserId") Long proctorUserId,
                                         @Param("startInclusive") LocalDateTime startInclusive,
                                         @Param("endExclusive") LocalDateTime endExclusive);
}
