CREATE TABLE IF NOT EXISTS audit_log_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_audit_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    action VARCHAR(128) NOT NULL,
    resource VARCHAR(128) NOT NULL,
    resource_id VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    `timestamp` DATETIME NOT NULL,
    details_json LONGTEXT NULL,
    archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_archive_timestamp (`timestamp`)
);

CREATE TABLE IF NOT EXISTS job_execution_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME NULL,
    CONSTRAINT fk_job_history_job FOREIGN KEY (job_id) REFERENCES job_record(id)
);

CREATE INDEX idx_job_record_status_nextretry ON job_record (status, next_retry_at);
