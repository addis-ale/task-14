package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "session_candidate")
@IdClass(SessionCandidateId.class)
public class SessionCandidate {

    @Id
    @Column(name = "session_id")
    private Long sessionId;

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
}
