package com.exam.system.dto;

import java.util.ArrayList;
import java.util.List;

public class ScopeDto {

    private List<Long> gradeIds = new ArrayList<>();
    private List<Long> classIds = new ArrayList<>();
    private List<Long> courseIds = new ArrayList<>();
    private List<Long> campusIds = new ArrayList<>();
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

    /** Alias for courseIds — frontend uses subjectIds */
    public List<Long> getSubjectIds() {
        return courseIds;
    }

    public void setSubjectIds(List<Long> subjectIds) {
        this.courseIds = subjectIds;
    }

    public List<Long> getCampusIds() {
        return campusIds;
    }

    public void setCampusIds(List<Long> campusIds) {
        this.campusIds = campusIds;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }
}
