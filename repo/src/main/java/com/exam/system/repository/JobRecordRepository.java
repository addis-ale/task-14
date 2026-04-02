package com.exam.system.repository;

import com.exam.system.entity.JobRecord;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRecordRepository extends JpaRepository<JobRecord, Long>, JpaSpecificationExecutor<JobRecord> {

    Optional<JobRecord> findByDedupKey(String dedupKey);

    List<JobRecord> findByJobTypeAndStatusAndNextRetryAtBefore(String jobType, String status, LocalDateTime now);

    @Query("""
            select j from JobRecord j
            where j.status in :statuses
              and (j.nextRetryAt is null or j.nextRetryAt <= :now)
            order by j.createdAt asc
            """)
    List<JobRecord> findSchedulable(@Param("statuses") List<String> statuses,
                                    @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            update JobRecord j
               set j.status = :newStatus
             where j.id = :jobId
               and j.status in :expectedStatuses
            """)
    int claim(@Param("jobId") Long jobId,
              @Param("newStatus") String newStatus,
              @Param("expectedStatuses") List<String> expectedStatuses);
}
