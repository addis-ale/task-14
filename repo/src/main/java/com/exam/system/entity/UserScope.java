package com.exam.system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_scope")
@IdClass(UserScopeId.class)
public class UserScope {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Id
    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Id
    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Id
    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Id
    @Column(name = "term_id", nullable = false)
    private Long termId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getTermId() {
        return termId;
    }

    public void setTermId(Long termId) {
        this.termId = termId;
    }
}
