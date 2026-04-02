package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.job.JobDetailResponse;
import com.exam.system.dto.job.JobExecutionHistoryResponse;
import com.exam.system.dto.job.JobSummaryResponse;
import com.exam.system.entity.JobExecutionHistory;
import com.exam.system.entity.JobRecord;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.job.JobStatus;
import com.exam.system.repository.JobExecutionHistoryRepository;
import com.exam.system.repository.JobRecordRepository;
import com.exam.system.service.JobMonitorService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobMonitorServiceImpl implements JobMonitorService {

    private final JobRecordRepository jobRecordRepository;
    private final JobExecutionHistoryRepository historyRepository;
    private final PageDataBuilder pageDataBuilder;

    public JobMonitorServiceImpl(JobRecordRepository jobRecordRepository,
                                 JobExecutionHistoryRepository historyRepository,
                                 PageDataBuilder pageDataBuilder) {
        this.jobRecordRepository = jobRecordRepository;
        this.historyRepository = historyRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<JobSummaryResponse> list(String jobType, String status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));

        Specification<JobRecord> spec = Specification.where(null);
        if (jobType != null && !jobType.isBlank()) {
            String normalized = jobType.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("jobType")), normalized));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.upper(root.get("status")), normalized));
        }

        Page<JobRecord> result = jobRecordRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return pageDataBuilder.from(result, this::toSummary, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse detail(Long jobId) {
        JobRecord job = findJob(jobId);
        JobDetailResponse response = toDetail(job);
        List<JobExecutionHistoryResponse> history = historyRepository
                .findByJobIdOrderByAttemptNumberAsc(jobId)
                .stream()
                .map(this::toHistory)
                .toList();
        response.setHistory(history);
        return response;
    }

    @Override
    @Transactional
    public void retry(Long jobId) {
        JobRecord job = findJob(jobId);
        if (JobStatus.RUNNING.equalsIgnoreCase(job.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Running job cannot be retried");
        }

        job.setStatus(JobStatus.PENDING);
        job.setAttempts(0);
        job.setNextRetryAt(LocalDateTime.now());
        job.setCompletedAt(null);
        job.setErrorMessage(null);
        jobRecordRepository.save(job);
    }

    private JobRecord findJob(Long jobId) {
        return jobRecordRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Job not found"));
    }

    private JobSummaryResponse toSummary(JobRecord job) {
        JobSummaryResponse response = new JobSummaryResponse();
        response.setId(job.getId());
        response.setJobType(job.getJobType());
        response.setDedupKey(job.getDedupKey());
        response.setStatus(job.getStatus());
        response.setAttempts(job.getAttempts());
        response.setNextRetryAt(job.getNextRetryAt());
        response.setCreatedAt(job.getCreatedAt());
        response.setCompletedAt(job.getCompletedAt());
        return response;
    }

    private JobDetailResponse toDetail(JobRecord job) {
        JobDetailResponse response = new JobDetailResponse();
        response.setId(job.getId());
        response.setJobType(job.getJobType());
        response.setDedupKey(job.getDedupKey());
        response.setStatus(job.getStatus());
        response.setAttempts(job.getAttempts());
        response.setNextRetryAt(job.getNextRetryAt());
        response.setCreatedAt(job.getCreatedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setPayloadJson(job.getPayloadJson());
        response.setErrorMessage(job.getErrorMessage());
        return response;
    }

    private JobExecutionHistoryResponse toHistory(JobExecutionHistory history) {
        JobExecutionHistoryResponse response = new JobExecutionHistoryResponse();
        response.setAttemptNumber(history.getAttemptNumber());
        response.setStatus(history.getStatus());
        response.setMessage(history.getMessage());
        response.setStartedAt(history.getStartedAt());
        response.setEndedAt(history.getEndedAt());
        return response;
    }
}
