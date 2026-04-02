package com.exam.system.job;

import com.exam.system.entity.JobExecutionHistory;
import com.exam.system.entity.JobRecord;
import com.exam.system.repository.JobExecutionHistoryRepository;
import com.exam.system.repository.JobRecordRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobSchedulerService {

    private final JobRecordRepository jobRecordRepository;
    private final JobExecutionHistoryRepository historyRepository;
    private final JobShardProperties shardProperties;
    private final Map<JobType, JobHandler> handlers = new HashMap<>();

    public JobSchedulerService(JobRecordRepository jobRecordRepository,
                               JobExecutionHistoryRepository historyRepository,
                               JobShardProperties shardProperties,
                               List<JobHandler> handlerList) {
        this.jobRecordRepository = jobRecordRepository;
        this.historyRepository = historyRepository;
        this.shardProperties = shardProperties;
        for (JobHandler handler : handlerList) {
            handlers.put(handler.supportedType(), handler);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndExecute() {
        List<JobRecord> candidates = jobRecordRepository.findSchedulable(
                List.of(JobStatus.PENDING), LocalDateTime.now());
        int processed = 0;
        for (JobRecord candidate : candidates) {
            if (processed >= Math.max(1, shardProperties.getPollBatchSize())) {
                break;
            }
            if (!isShardOwner(candidate)) {
                continue;
            }

            int claimed = jobRecordRepository.claim(candidate.getId(), JobStatus.RUNNING, List.of(JobStatus.PENDING));
            if (claimed == 0) {
                continue;
            }
            JobRecord claimedJob = jobRecordRepository.findById(candidate.getId()).orElse(null);
            if (claimedJob == null) {
                continue;
            }
            executeOne(claimedJob);
            processed++;
        }
    }

    private void executeOne(JobRecord job) {
        int attemptNumber = job.getAttempts() + 1;
        JobExecutionHistory history = new JobExecutionHistory();
        history.setJobId(job.getId());
        history.setAttemptNumber(attemptNumber);
        history.setStatus(JobStatus.RUNNING);
        history.setStartedAt(LocalDateTime.now());
        historyRepository.save(history);

        try {
            JobType type = JobType.valueOf(job.getJobType());
            JobHandler handler = handlers.get(type);
            if (handler == null) {
                throw new IllegalStateException("No handler for job type " + type);
            }
            handler.handle(job);
            job.setAttempts(attemptNumber);
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            history.setStatus(JobStatus.COMPLETED);
            history.setMessage("ok");
        } catch (Exception ex) {
            job.setAttempts(attemptNumber);
            if (attemptNumber >= 3) {
                job.setStatus(JobStatus.FAILED);
                job.setCompletedAt(LocalDateTime.now());
            } else {
                job.setStatus(JobStatus.PENDING);
                job.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds(attemptNumber)));
            }
            job.setErrorMessage(ex.getMessage());
            history.setStatus(JobStatus.FAILED);
            history.setMessage(ex.getMessage());
        }

        history.setEndedAt(LocalDateTime.now());
        historyRepository.save(history);
        jobRecordRepository.save(job);
    }

    private long backoffSeconds(int attempt) {
        return switch (attempt) {
            case 1 -> 30;
            case 2 -> 120;
            default -> 480;
        };
    }

    private boolean isShardOwner(JobRecord job) {
        int shards = Math.max(1, shardProperties.getTotalShards());
        int index = Math.max(0, shardProperties.getNodeIndex());
        String key = job.getJobType() + ":" + (job.getDedupKey() == null ? "" : job.getDedupKey());
        int owner = Math.floorMod(key.hashCode(), shards);
        return owner == index;
    }
}
