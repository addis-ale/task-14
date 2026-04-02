package com.exam.system.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.job")
public class JobShardProperties {

    private int nodeIndex;
    private int totalShards;
    private int pollBatchSize;

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public int getTotalShards() {
        return totalShards;
    }

    public void setTotalShards(int totalShards) {
        this.totalShards = totalShards;
    }

    public int getPollBatchSize() {
        return pollBatchSize;
    }

    public void setPollBatchSize(int pollBatchSize) {
        this.pollBatchSize = pollBatchSize;
    }
}
