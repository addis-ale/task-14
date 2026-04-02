package com.exam.system.dto.session;

import java.time.LocalDateTime;

public class SessionProctorResponse {

    private Long id;
    private Long proctorUserId;
    private Long roomId;
    private LocalDateTime timeSlotStart;
    private LocalDateTime timeSlotEnd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
