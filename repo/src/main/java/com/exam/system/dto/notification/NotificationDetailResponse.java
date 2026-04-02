package com.exam.system.dto.notification;

public class NotificationDetailResponse extends NotificationSummaryResponse {

    private String body;
    private NotificationTargetScopeDto targetScope;
    private DeliveryStatsDto deliveryStats;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public NotificationTargetScopeDto getTargetScope() {
        return targetScope;
    }

    public void setTargetScope(NotificationTargetScopeDto targetScope) {
        this.targetScope = targetScope;
    }

    public DeliveryStatsDto getDeliveryStats() {
        return deliveryStats;
    }

    public void setDeliveryStats(DeliveryStatsDto deliveryStats) {
        this.deliveryStats = deliveryStats;
    }
}
