package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.job.JobDetailResponse;
import com.exam.system.dto.job.JobSummaryResponse;

public interface JobMonitorService {

    PageData<JobSummaryResponse> list(String jobType, String status, int page, int size);

    JobDetailResponse detail(Long jobId);

    void retry(Long jobId);
}
