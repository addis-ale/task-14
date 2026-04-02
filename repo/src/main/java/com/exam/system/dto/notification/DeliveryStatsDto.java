package com.exam.system.dto.notification;

public class DeliveryStatsDto {

    private long total;
    private long delivered;
    private long pending;
    private long failed;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getDelivered() {
        return delivered;
    }

    public void setDelivered(long delivered) {
        this.delivered = delivered;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }
}
