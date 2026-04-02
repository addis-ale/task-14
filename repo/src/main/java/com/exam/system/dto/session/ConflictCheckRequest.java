package com.exam.system.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ConflictCheckRequest {

    private Long excludeSessionId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @Valid
    private List<SessionCandidateRequest> candidates = new ArrayList<>();

    @Valid
    private List<SessionRoomAssignmentRequest> roomAssignments = new ArrayList<>();

    @Valid
    private List<SessionProctorRequest> proctors = new ArrayList<>();

    public Long getExcludeSessionId() {
        return excludeSessionId;
    }

    public void setExcludeSessionId(Long excludeSessionId) {
        this.excludeSessionId = excludeSessionId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public List<SessionCandidateRequest> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<SessionCandidateRequest> candidates) {
        this.candidates = candidates;
    }

    public List<SessionRoomAssignmentRequest> getRoomAssignments() {
        return roomAssignments;
    }

    public void setRoomAssignments(List<SessionRoomAssignmentRequest> roomAssignments) {
        this.roomAssignments = roomAssignments;
    }

    public List<SessionProctorRequest> getProctors() {
        return proctors;
    }

    public void setProctors(List<SessionProctorRequest> proctors) {
        this.proctors = proctors;
    }
}
