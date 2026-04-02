package com.exam.system.job;

import com.exam.system.entity.JobRecord;

public interface JobHandler {

    JobType supportedType();

    void handle(JobRecord jobRecord);
}
