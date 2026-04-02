package com.exam.system.dto.session;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SessionUpsertRequest {

    @NotNull
    private Long termId;

    @NotNull
    private Long subjectId;

    @NotNull
    private Long gradeId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private String status;

    @Valid
    private List<SessionRoomAssignmentRequest> roomAssignments = new ArrayList<>();

    @Valid
    private List<SessionCandidateRequest> candidates = new ArrayList<>();

    @Valid
    private List<SessionProctorRequest> proctors = new ArrayList<>();

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SessionRoomAssignmentRequest> getRoomAssignments() {
        return roomAssignments;
    }

    public void setRoomAssignments(List<SessionRoomAssignmentRequest> roomAssignments) {
        this.roomAssignments = roomAssignments;
    }

    public List<SessionCandidateRequest> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<SessionCandidateRequest> candidates) {
        this.candidates = candidates;
    }

    public List<SessionProctorRequest> getProctors() {
        return proctors;
    }

    public void setProctors(List<SessionProctorRequest> proctors) {
        this.proctors = proctors;
    }
}
