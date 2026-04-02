package com.exam.system.dto.session;

public class SessionRoomAssignmentResponse {

    private Long roomId;
    private Integer assignedCount;
    private Integer capacity;
    private Integer remainingCapacity;

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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(Integer remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }
}
