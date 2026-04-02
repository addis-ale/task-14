package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_assignment")
@IdClass(RoomAssignmentId.class)
public class RoomAssignment {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "assigned_count", nullable = false)
    private Integer assignedCount;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

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
