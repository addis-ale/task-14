CREATE TABLE IF NOT EXISTS request_nonce (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nonce VARCHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS password_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_password_history_user_time (user_id, created_at),
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE IF NOT EXISTS user_security_policy (
    user_id BIGINT PRIMARY KEY,
    concurrent_sessions_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_security_policy_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);
