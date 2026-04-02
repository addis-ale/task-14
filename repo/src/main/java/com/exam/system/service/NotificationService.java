package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.notification.DeliveryResponse;
import com.exam.system.dto.notification.NotificationCreateRequest;
import com.exam.system.dto.notification.NotificationDetailResponse;
import com.exam.system.dto.notification.NotificationSummaryResponse;

public interface NotificationService {

    PageData<NotificationSummaryResponse> list(String eventType, String status, int page, int size);

    NotificationDetailResponse detail(Long notificationId);

    NotificationSummaryResponse create(NotificationCreateRequest request);

    void submitReview(Long notificationId);

    void publish(Long notificationId);

    void delete(Long notificationId);

    PageData<DeliveryResponse> listDeliveries(Long notificationId, String status, int page, int size);

    void retryDelivery(Long notificationId, Long deliveryId);
}
