package com.exam.system.dto.version;

public class VersionDetailResponse extends VersionSummaryResponse {

    private String snapshotJson;

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }
}
