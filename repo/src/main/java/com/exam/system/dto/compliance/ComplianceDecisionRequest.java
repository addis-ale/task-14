package com.exam.system.dto.compliance;

import jakarta.validation.constraints.NotBlank;

public class ComplianceDecisionRequest {

    @NotBlank
    private String comments;

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
