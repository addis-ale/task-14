package com.exam.system.dto.draft;

import jakarta.validation.constraints.NotBlank;

public class DraftSaveRequest {

    @NotBlank(message = "Draft JSON is required")
    private String draftJson;

    public String getDraftJson() { return draftJson; }
    public void setDraftJson(String draftJson) { this.draftJson = draftJson; }
}
