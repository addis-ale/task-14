package com.exam.system.service.impl.notification;

import com.exam.system.entity.JobRecord;
import com.exam.system.repository.JobRecordRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobRecordService {

    private final JobRecordRepository jobRecordRepository;

    public JobRecordService(JobRecordRepository jobRecordRepository) {
        this.jobRecordRepository = jobRecordRepository;
    }

    @Transactional
    public JobRecord create(String jobType, String dedupKey, String payload, LocalDateTime nextRetryAt) {
        Optional<JobRecord> existing = jobRecordRepository.findByDedupKey(dedupKey);
        if (existing.isPresent()) {
            return existing.get();
        }
        JobRecord job = new JobRecord();
        job.setJobType(jobType);
        job.setDedupKey(dedupKey);
        job.setStatus("PENDING");
        job.setPayloadJson(payload);
        job.setAttempts(0);
        job.setNextRetryAt(nextRetryAt);
        return jobRecordRepository.save(job);
    }

    @Transactional
    public void complete(JobRecord job) {
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        jobRecordRepository.save(job);
    }

    @Transactional
    public void fail(JobRecord job, String error) {
        job.setStatus("FAILED");
        job.setErrorMessage(error);
        job.setCompletedAt(LocalDateTime.now());
        jobRecordRepository.save(job);
    }
}
