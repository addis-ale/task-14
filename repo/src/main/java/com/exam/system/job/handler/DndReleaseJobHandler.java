package com.exam.system.job.handler;

import com.exam.system.entity.JobRecord;
import com.exam.system.job.JobHandler;
import com.exam.system.job.JobType;
import com.exam.system.service.impl.notification.NotificationDeliveryPipeline;
import org.springframework.stereotype.Component;

@Component
public class DndReleaseJobHandler implements JobHandler {

    private final NotificationDeliveryPipeline pipeline;

    public DndReleaseJobHandler(NotificationDeliveryPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public JobType supportedType() {
        return JobType.DND_RELEASE;
    }

    @Override
    public void handle(JobRecord jobRecord) {
        pipeline.executeDndRelease(jobRecord);
    }
}
