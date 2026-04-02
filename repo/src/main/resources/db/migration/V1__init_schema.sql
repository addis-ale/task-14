CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    locked_until DATETIME NULL,
    device_token VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE IF NOT EXISTS user_scope (
    user_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL DEFAULT 0,
    class_id BIGINT NOT NULL DEFAULT 0,
    course_id BIGINT NOT NULL DEFAULT 0,
    term_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id, grade_id, class_id, course_id, term_id),
    CONSTRAINT fk_user_scope_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS role_permission (
    role_id BIGINT NOT NULL,
    resource VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    PRIMARY KEY (role_id, resource, action),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE IF NOT EXISTS sys_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    active_role VARCHAR(64) NOT NULL,
    device_fingerprint VARCHAR(255) NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sys_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS login_attempt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    ip_address VARCHAR(64) NULL,
    device_fingerprint VARCHAR(255) NULL,
    success BOOLEAN NOT NULL,
    attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_login_attempt_user_time (user_id, attempted_at),
    CONSTRAINT fk_login_attempt_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS academic_term (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS campus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NULL
);

CREATE TABLE IF NOT EXISTS exam_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    campus_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT fk_exam_room_campus FOREIGN KEY (campus_id) REFERENCES campus(id)
);

CREATE TABLE IF NOT EXISTS subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    grade_id BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS exam_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    term_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_exam_session_term FOREIGN KEY (term_id) REFERENCES academic_term(id),
    CONSTRAINT fk_exam_session_subject FOREIGN KEY (subject_id) REFERENCES subject(id),
    CONSTRAINT fk_exam_session_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS session_candidate (
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    seat_number INT NOT NULL,
    PRIMARY KEY (session_id, student_id),
    CONSTRAINT fk_session_candidate_session FOREIGN KEY (session_id) REFERENCES exam_session(id),
    CONSTRAINT fk_session_candidate_student FOREIGN KEY (student_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS room_assignment (
    session_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    assigned_count INT NOT NULL,
    PRIMARY KEY (session_id, room_id),
    CONSTRAINT fk_room_assignment_session FOREIGN KEY (session_id) REFERENCES exam_session(id),
    CONSTRAINT fk_room_assignment_room FOREIGN KEY (room_id) REFERENCES exam_room(id)
);

CREATE TABLE IF NOT EXISTS proctor_assign (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    proctor_user_id BIGINT NOT NULL,
    time_slot_start DATETIME NOT NULL,
    time_slot_end DATETIME NOT NULL,
    CONSTRAINT fk_proctor_assign_session FOREIGN KEY (session_id) REFERENCES exam_session(id),
    CONSTRAINT fk_proctor_assign_room FOREIGN KEY (room_id) REFERENCES exam_room(id),
    CONSTRAINT fk_proctor_assign_user FOREIGN KEY (proctor_user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    target_scope JSON NULL,
    status VARCHAR(32) NOT NULL,
    published_by BIGINT NULL,
    compliance_status VARCHAR(32) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_publisher FOREIGN KEY (published_by) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS notif_preference (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    dnd_start TIME NULL,
    dnd_end TIME NULL,
    CONSTRAINT fk_notif_preference_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS notif_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    notification_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_attempt_at DATETIME NULL,
    delivered_at DATETIME NULL,
    CONSTRAINT fk_notif_delivery_notification FOREIGN KEY (notification_id) REFERENCES notification(id),
    CONSTRAINT fk_notif_delivery_recipient FOREIGN KEY (recipient_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS entity_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(64) NOT NULL,
    entity_id BIGINT NOT NULL,
    term_id BIGINT NULL,
    version_number INT NOT NULL,
    snapshot_json LONGTEXT NOT NULL,
    changed_by BIGINT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entity_version_user FOREIGN KEY (changed_by) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(128) NOT NULL,
    resource VARCHAR(128) NOT NULL,
    resource_id VARCHAR(128) NULL,
    ip_address VARCHAR(64) NULL,
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details_json LONGTEXT NULL,
    CONSTRAINT fk_audit_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS job_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_type VARCHAR(64) NOT NULL,
    dedup_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    payload_json TEXT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    error_message TEXT NULL
);

CREATE TABLE IF NOT EXISTS import_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    total_rows INT NOT NULL,
    valid_rows INT NOT NULL,
    invalid_rows INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_report_json LONGTEXT NULL,
    uploaded_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_batch_user FOREIGN KEY (uploaded_by) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS anti_cheat_flag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    flag_type VARCHAR(64) NOT NULL,
    details_json TEXT NULL,
    review_status VARCHAR(32) NOT NULL,
    reviewer_id BIGINT NULL,
    decision VARCHAR(128) NULL,
    decided_at DATETIME NULL,
    CONSTRAINT fk_anti_cheat_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_anti_cheat_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS compliance_review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content_type VARCHAR(64) NOT NULL,
    content_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewer_id BIGINT NULL,
    comments TEXT NULL,
    decided_at DATETIME NULL,
    CONSTRAINT fk_compliance_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS auto_save_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    form_key VARCHAR(128) NOT NULL,
    draft_json LONGTEXT NOT NULL,
    saved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auto_save_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
