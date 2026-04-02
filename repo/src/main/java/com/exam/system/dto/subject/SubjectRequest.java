package com.exam.system.dto.subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubjectRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long gradeId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getGradeId() {
        return gradeId;
    }

    public void setGradeId(Long gradeId) {
        this.gradeId = gradeId;
    }
}
