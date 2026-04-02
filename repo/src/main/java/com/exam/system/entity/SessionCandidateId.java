package com.exam.system.entity;

import java.io.Serializable;
import java.util.Objects;

public class SessionCandidateId implements Serializable {

    private Long sessionId;
    private Long studentId;

    public SessionCandidateId() {
    }

    public SessionCandidateId(Long sessionId, Long studentId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SessionCandidateId that)) {
            return false;
        }
        return Objects.equals(sessionId, that.sessionId) && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, studentId);
    }
}
