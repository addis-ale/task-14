package com.exam.system.dto.session;

public class ConflictItemResponse {

    private Long targetId;
    private Long existingSessionId;
    private String conflictType;

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getExistingSessionId() {
        return existingSessionId;
    }

    public void setExistingSessionId(Long existingSessionId) {
        this.existingSessionId = existingSessionId;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }
}
