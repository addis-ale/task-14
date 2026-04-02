package com.exam.system.repository;

import com.exam.system.entity.ExamRoom;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRoomRepository extends JpaRepository<ExamRoom, Long> {

    Page<ExamRoom> findByCampusId(Long campusId, Pageable pageable);

    List<ExamRoom> findByIdIn(List<Long> roomIds);

    long countByCampusId(Long campusId);
}
