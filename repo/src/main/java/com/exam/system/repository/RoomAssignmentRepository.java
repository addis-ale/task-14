package com.exam.system.repository;

import com.exam.system.entity.RoomAssignment;
import com.exam.system.entity.RoomAssignmentId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAssignmentRepository extends JpaRepository<RoomAssignment, RoomAssignmentId> {

    List<RoomAssignment> findBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    long countByRoomId(Long roomId);

    @Query("""
            select ra.roomId as roomId, es.id as sessionId
            from RoomAssignment ra, ExamSession es
            where ra.sessionId = es.id
              and ra.roomId in :roomIds
              and es.date = :date
              and es.startTime < :endTime
              and es.endTime > :startTime
              and (:excludeSessionId is null or es.id <> :excludeSessionId)
            """)
    List<RoomConflictProjection> findRoomConflicts(@Param("roomIds") List<Long> roomIds,
                                                   @Param("date") LocalDate date,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime,
                                                   @Param("excludeSessionId") Long excludeSessionId);
}
