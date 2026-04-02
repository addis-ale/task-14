package com.exam.system.repository;

import com.exam.system.entity.JobExecutionHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionHistoryRepository extends JpaRepository<JobExecutionHistory, Long> {

    List<JobExecutionHistory> findByJobIdOrderByAttemptNumberAsc(Long jobId);
}
