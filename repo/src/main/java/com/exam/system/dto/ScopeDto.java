package com.exam.system.dto;

import java.util.ArrayList;
import java.util.List;

public class ScopeDto {

    private List<Long> gradeIds = new ArrayList<>();
    private List<Long> classIds = new ArrayList<>();
    private List<Long> courseIds = new ArrayList<>();
    private Long termId;

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

    public List<Long> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<Long> courseIds) {
        this.courseIds = courseIds;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }
}
