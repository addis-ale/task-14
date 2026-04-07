package com.exam.system.job.handler;

import com.exam.system.entity.ImportBatch;
import com.exam.system.entity.JobRecord;
import com.exam.system.job.JobHandler;
import com.exam.system.job.JobType;
import com.exam.system.repository.ImportBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BulkImportJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(BulkImportJobHandler.class);
    private final ImportBatchRepository importBatchRepository;

    public BulkImportJobHandler(ImportBatchRepository importBatchRepository) {
        this.importBatchRepository = importBatchRepository;
    }

    @Override
    public JobType supportedType() {
        return JobType.BULK_IMPORT;
    }

    @Override
    public void handle(JobRecord job) {
        log.info("Processing bulk import job: {}", job.getId());
        try {
            Long batchId = Long.parseLong(job.getPayloadJson());
            ImportBatch batch = importBatchRepository.findById(batchId)
                    .orElseThrow(() -> new RuntimeException("Import batch not found: " + batchId));

            if (!"COMMITTED".equals(batch.getStatus())) {
                log.warn("Batch {} is not in COMMITTED status, skipping", batchId);
                return;
            }

            // Process the committed batch: apply rows to domain tables
            batch.setStatus("PROCESSING");
            importBatchRepository.save(batch);

            // Domain-specific import logic would go here based on entityType
            // For SESSION_CANDIDATE: parse rows and insert into session_candidate table
            // For ROSTER: parse rows and insert/update student records

            batch.setStatus("COMPLETED");
            importBatchRepository.save(batch);
            log.info("Bulk import job {} completed for batch {}", job.getId(), batchId);
        } catch (Exception e) {
            log.error("Bulk import job {} failed: {}", job.getId(), e.getMessage(), e);
            throw new RuntimeException("Import processing failed", e);
        }
    }
}
