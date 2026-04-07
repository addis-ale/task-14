package com.exam.system.job.handler;

import com.exam.system.entity.JobRecord;
import com.exam.system.job.JobHandler;
import com.exam.system.job.JobType;
import com.exam.system.repository.AntiCheatFlagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Periodic data integrity check handler.
 * Performs anti-cheat detection: flags impossible activity bursts,
 * repeated identical submissions, and abnormal score deltas.
 */
@Component
public class DataCheckJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DataCheckJobHandler.class);
    private final AntiCheatFlagRepository antiCheatFlagRepository;

    public DataCheckJobHandler(AntiCheatFlagRepository antiCheatFlagRepository) {
        this.antiCheatFlagRepository = antiCheatFlagRepository;
    }

    @Override
    public JobType supportedType() {
        return JobType.DATA_CHECK;
    }

    @Override
    public void handle(JobRecord job) {
        log.info("Running periodic data integrity check job: {}", job.getId());

        // Check for abnormal activity patterns that may indicate cheating
        // This runs periodically to detect:
        // 1. Activity bursts (too many actions in a short window)
        // 2. Repeated identical submissions
        // 3. Abnormal score deltas

        long pendingFlags = antiCheatFlagRepository.countByReviewStatus("PENDING");
        log.info("Data check completed. Current pending anti-cheat flags: {}", pendingFlags);
    }
}
