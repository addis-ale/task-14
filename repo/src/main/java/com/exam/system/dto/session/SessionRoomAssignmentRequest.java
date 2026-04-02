package com.exam.system.dto.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SessionRoomAssignmentRequest {

    @NotNull
    private Long roomId;

    @NotNull
    @Min(0)
    private Integer assignedCount;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Integer getAssignedCount() {
        return assignedCount;
    }

    public void setAssignedCount(Integer assignedCount) {
        this.assignedCount = assignedCount;
    }
}
