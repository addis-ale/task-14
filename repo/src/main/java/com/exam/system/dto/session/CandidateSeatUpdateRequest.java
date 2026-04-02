package com.exam.system.dto.session;

import jakarta.validation.constraints.NotNull;

public class CandidateSeatUpdateRequest {

    @NotNull
    private Integer seatNumber;

    @NotNull
    private Long roomId;

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
