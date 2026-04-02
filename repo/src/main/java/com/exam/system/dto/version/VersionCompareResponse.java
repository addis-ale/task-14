package com.exam.system.dto.version;

import java.util.ArrayList;
import java.util.List;

public class VersionCompareResponse {

    private Long versionA;
    private Long versionB;
    private List<FieldDiffResponse> diffs = new ArrayList<>();

    public Long getVersionA() {
        return versionA;
    }

    public void setVersionA(Long versionA) {
        this.versionA = versionA;
    }

    public Long getVersionB() {
        return versionB;
    }

    public void setVersionB(Long versionB) {
        this.versionB = versionB;
    }

    public List<FieldDiffResponse> getDiffs() {
        return diffs;
    }

    public void setDiffs(List<FieldDiffResponse> diffs) {
        this.diffs = diffs;
    }
}
