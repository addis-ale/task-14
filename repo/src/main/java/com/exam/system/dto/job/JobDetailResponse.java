package com.exam.system.dto.job;

import java.util.ArrayList;
import java.util.List;

public class JobDetailResponse extends JobSummaryResponse {

    private String payloadJson;
    private String errorMessage;
    private List<JobExecutionHistoryResponse> history = new ArrayList<>();

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<JobExecutionHistoryResponse> getHistory() {
        return history;
    }

    public void setHistory(List<JobExecutionHistoryResponse> history) {
        this.history = history;
    }
}
