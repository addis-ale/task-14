package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log_archive")
public class AuditLogArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_audit_id", nullable = false)
    private Long sourceAuditId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(nullable = false, length = 128)
    private String resource;

    @Column(name = "resource_id", length = 128)
    private String resourceId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "details_json", columnDefinition = "LONGTEXT")
    private String detailsJson;

    @Column(name = "archived_at", nullable = false)
    private LocalDateTime archivedAt;

    @PrePersist
    public void prePersist() {
        if (archivedAt == null) {
            archivedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSourceAuditId() {
        return sourceAuditId;
    }

    public void setSourceAuditId(Long sourceAuditId) {
        this.sourceAuditId = sourceAuditId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
