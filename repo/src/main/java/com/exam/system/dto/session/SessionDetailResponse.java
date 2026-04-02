package com.exam.system.dto.session;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SessionDetailResponse {

    private Long id;
    private Long termId;
    private Long subjectId;
    private Long gradeId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private List<SessionCandidateResponse> candidates = new ArrayList<>();
    private List<SessionRoomAssignmentResponse> roomAssignments = new ArrayList<>();
    private List<SessionProctorResponse> proctors = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<SessionCandidateResponse> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<SessionCandidateResponse> candidates) {
        this.candidates = candidates;
    }

    public List<SessionRoomAssignmentResponse> getRoomAssignments() {
        return roomAssignments;
    }

    public void setRoomAssignments(List<SessionRoomAssignmentResponse> roomAssignments) {
        this.roomAssignments = roomAssignments;
    }

    public List<SessionProctorResponse> getProctors() {
        return proctors;
    }

    public void setProctors(List<SessionProctorResponse> proctors) {
        this.proctors = proctors;
    }
}
