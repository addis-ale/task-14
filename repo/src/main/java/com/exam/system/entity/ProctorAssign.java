package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "proctor_assign")
public class ProctorAssign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "proctor_user_id", nullable = false)
    private Long proctorUserId;

    @Column(name = "time_slot_start", nullable = false)
    private LocalDateTime timeSlotStart;

    @Column(name = "time_slot_end", nullable = false)
    private LocalDateTime timeSlotEnd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getProctorUserId() {
        return proctorUserId;
    }

    public void setProctorUserId(Long proctorUserId) {
        this.proctorUserId = proctorUserId;
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
