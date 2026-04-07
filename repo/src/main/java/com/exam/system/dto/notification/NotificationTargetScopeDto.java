package com.exam.system.dto.notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationTargetScopeDto {

    private Long gradeId;
    private Long subjectId;
    private List<Long> gradeIds = new ArrayList<>();
    private List<Long> classIds = new ArrayList<>();

    public Long getGradeId() {
        // Prefer explicit gradeId; fall back to first element of gradeIds
        if (gradeId != null) return gradeId;
        return gradeIds.isEmpty() ? null : gradeIds.get(0);
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public List<Long> getGradeIds() {
        return gradeIds;
    }

    public void setGradeIds(List<Long> gradeIds) {
        this.gradeIds = gradeIds;
    }

    public List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<Long> classIds) {
        this.classIds = classIds;
    }
}
