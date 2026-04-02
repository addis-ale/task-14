package com.exam.system.notification;

public class NotificationPublishedEvent {

    private final Long notificationId;

    public NotificationPublishedEvent(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getNotificationId() {
        return notificationId;
    }
}
