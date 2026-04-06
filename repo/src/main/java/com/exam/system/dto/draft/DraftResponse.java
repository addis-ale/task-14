package com.exam.system.dto.draft;

import java.time.LocalDateTime;

public class DraftResponse {

    private String formKey;
    private String draftJson;
    private LocalDateTime savedAt;

    public String getFormKey() { return formKey; }
    public void setFormKey(String formKey) { this.formKey = formKey; }
    public String getDraftJson() { return draftJson; }
    public void setDraftJson(String draftJson) { this.draftJson = draftJson; }
    public LocalDateTime getSavedAt() { return savedAt; }
    public void setSavedAt(LocalDateTime savedAt) { this.savedAt = savedAt; }
}
