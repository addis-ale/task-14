package com.exam.system.dto.session;

import jakarta.validation.constraints.NotNull;

public class SessionCandidateRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long roomId;

    @NotNull
    private Integer seatNumber;

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
