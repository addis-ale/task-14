package com.exam.system.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserScopeId implements Serializable {

    private Long userId;
    private Long gradeId;
    private Long classId;
    private Long courseId;
    private Long termId;

    public UserScopeId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserScopeId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(gradeId, that.gradeId)
                && Objects.equals(classId, that.classId)
                && Objects.equals(courseId, that.courseId)
                && Objects.equals(termId, that.termId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, gradeId, classId, courseId, termId);
    }
}
