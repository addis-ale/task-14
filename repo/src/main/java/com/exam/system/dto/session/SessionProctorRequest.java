package com.exam.system.dto.session;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class SessionProctorRequest {

    @NotNull
    private Long proctorUserId;

    @NotNull
    private Long roomId;

    @NotNull
    private LocalDateTime timeSlotStart;

    @NotNull
    private LocalDateTime timeSlotEnd;

    public Long getProctorUserId() {
        return proctorUserId;
    }

    public void setProctorUserId(Long proctorUserId) {
        this.proctorUserId = proctorUserId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getTimeSlotStart() {
        return timeSlotStart;
    }

    public void setTimeSlotStart(LocalDateTime timeSlotStart) {
        this.timeSlotStart = timeSlotStart;
    }

    public LocalDateTime getTimeSlotEnd() {
        return timeSlotEnd;
    }

    public void setTimeSlotEnd(LocalDateTime timeSlotEnd) {
        this.timeSlotEnd = timeSlotEnd;
    }
}
