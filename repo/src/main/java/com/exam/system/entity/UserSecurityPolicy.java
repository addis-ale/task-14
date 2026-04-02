package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_security_policy")
public class UserSecurityPolicy {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "concurrent_sessions_allowed", nullable = false)
    private boolean concurrentSessionsAllowed;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isConcurrentSessionsAllowed() {
        return concurrentSessionsAllowed;
    }

    public void setConcurrentSessionsAllowed(boolean concurrentSessionsAllowed) {
        this.concurrentSessionsAllowed = concurrentSessionsAllowed;
    }
}
