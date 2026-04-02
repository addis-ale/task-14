package com.exam.system.dto.compliance;

public class ComplianceReviewDetailResponse extends ComplianceReviewSummaryResponse {

    private String contentTitle;
    private String contentBody;

    public String getContentTitle() {
        return contentTitle;
    }

    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }

    public String getContentBody() {
        return contentBody;
    }

    public void setContentBody(String contentBody) {
        this.contentBody = contentBody;
    }
}
