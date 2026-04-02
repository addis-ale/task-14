package com.exam.system.job.handler;

import com.exam.system.entity.JobRecord;
import com.exam.system.job.JobHandler;
import com.exam.system.job.JobType;
import com.exam.system.service.impl.notification.NotificationDeliveryPipeline;
import org.springframework.stereotype.Component;

@Component
public class RetryDeliveryJobHandler implements JobHandler {

    private final NotificationDeliveryPipeline pipeline;

    public RetryDeliveryJobHandler(NotificationDeliveryPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public JobType supportedType() {
        return JobType.RETRY_DELIVERY;
    }

    @Override
    public void handle(JobRecord jobRecord) {
        pipeline.executeRetryDelivery(jobRecord);
    }
}
