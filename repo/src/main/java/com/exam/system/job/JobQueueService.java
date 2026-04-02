package com.exam.system.job;

import com.exam.system.entity.JobRecord;
import com.exam.system.repository.JobRecordRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobQueueService {

    private final JobRecordRepository jobRecordRepository;

    public JobQueueService(JobRecordRepository jobRecordRepository) {
        this.jobRecordRepository = jobRecordRepository;
    }

    @Transactional
    public JobRecord enqueue(JobType type, String dedupKey, String payloadJson) {
        return enqueue(type, dedupKey, payloadJson, LocalDateTime.now());
    }

    @Transactional
    public JobRecord enqueue(JobType type, String dedupKey, String payloadJson, LocalDateTime nextRetryAt) {
        return jobRecordRepository.findByDedupKey(dedupKey).orElseGet(() -> {
            JobRecord job = new JobRecord();
            job.setJobType(type.name());
            job.setDedupKey(dedupKey);
            job.setStatus(JobStatus.PENDING);
            job.setPayloadJson(payloadJson);
            job.setAttempts(0);
            job.setNextRetryAt(nextRetryAt);
            return jobRecordRepository.save(job);
        });
    }
}
