package com.exam.system.dto.session;

import java.util.ArrayList;
import java.util.List;

public class ConflictReportResponse {

    private List<ConflictItemResponse> conflicts = new ArrayList<>();

    public List<ConflictItemResponse> getConflicts() {
        return conflicts;
    }

    public void setConflicts(List<ConflictItemResponse> conflicts) {
        this.conflicts = conflicts;
    }

    public boolean hasConflict() {
        return !conflicts.isEmpty();
    }
}
