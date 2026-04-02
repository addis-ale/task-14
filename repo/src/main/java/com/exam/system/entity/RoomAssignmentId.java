package com.exam.system.entity;

import java.io.Serializable;
import java.util.Objects;

public class RoomAssignmentId implements Serializable {

    private Long sessionId;
    private Long roomId;

    public RoomAssignmentId() {
    }

    public RoomAssignmentId(Long sessionId, Long roomId) {
        this.sessionId = sessionId;
        this.roomId = roomId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoomAssignmentId that)) {
            return false;
        }
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(roomId, that.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, roomId);
    }
}
