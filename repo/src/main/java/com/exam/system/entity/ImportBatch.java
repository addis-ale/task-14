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
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "valid_rows", nullable = false)
    private Integer validRows;

    @Column(name = "invalid_rows", nullable = false)
    private Integer invalidRows;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "error_report_json", columnDefinition = "LONGTEXT")
    private String errorReportJson;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getValidRows() {
        return validRows;
    }

    public void setValidRows(Integer validRows) {
        this.validRows = validRows;
    }

    public Integer getInvalidRows() {
        return invalidRows;
    }

    public void setInvalidRows(Integer invalidRows) {
        this.invalidRows = invalidRows;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorReportJson() {
        return errorReportJson;
    }

    public void setErrorReportJson(String errorReportJson) {
        this.errorReportJson = errorReportJson;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
