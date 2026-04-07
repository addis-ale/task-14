package com.exam.system.dto.session;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SessionCandidateResponse {

    private Long studentId;
    private String maskedStudentId;
    private Long roomId;
    private Integer seatNumber;

    @JsonIgnore
    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getMaskedStudentId() {
        return maskedStudentId;
    }

    public void setMaskedStudentId(String maskedStudentId) {
        this.maskedStudentId = maskedStudentId;
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
