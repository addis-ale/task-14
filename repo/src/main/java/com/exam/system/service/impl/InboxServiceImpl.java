package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.inbox.InboxMessageResponse;
import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.Notification;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.notification.DeliveryStatus;
import com.exam.system.repository.NotifDeliveryRepository;
import com.exam.system.repository.NotificationRepository;
import com.exam.system.service.InboxService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxServiceImpl implements InboxService {

    private final NotifDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final PageDataBuilder pageDataBuilder;

    public InboxServiceImpl(NotifDeliveryRepository deliveryRepository,
                            NotificationRepository notificationRepository,
                            PageDataBuilder pageDataBuilder) {
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<InboxMessageResponse> list(Long userId, Boolean read, int page, int size) {
        Page<NotifDelivery> deliveries;
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size));
        if (read == null) {
            deliveries = deliveryRepository.findByRecipientIdAndStatus(userId, DeliveryStatus.DELIVERED, pageable);
        } else if (read) {
            deliveries = deliveryRepository.findByRecipientIdAndStatusAndReadAtIsNotNull(userId, DeliveryStatus.DELIVERED, pageable);
        } else {
            deliveries = deliveryRepository.findByRecipientIdAndStatusAndReadAtIsNull(userId, DeliveryStatus.DELIVERED, pageable);
        }

        List<Long> notificationIds = deliveries.getContent().stream().map(NotifDelivery::getNotificationId).distinct().toList();
        Map<Long, Notification> notifications = new HashMap<>();
        if (!notificationIds.isEmpty()) {
            notificationRepository.findAllById(notificationIds)
                    .forEach(notification -> notifications.put(notification.getId(), notification));
        }

        return pageDataBuilder.from(deliveries, delivery -> toInboxMessage(delivery, notifications.get(delivery.getNotificationId())), page, size);
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long deliveryId) {
        NotifDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Inbox message not found"));
        if (!delivery.getRecipientId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN, "Cannot mark other user's message");
        }
        delivery.setReadAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        List<NotifDelivery> unread = deliveryRepository.findByRecipientIdAndStatusAndReadAtIsNull(userId, DeliveryStatus.DELIVERED);
        LocalDateTime now = LocalDateTime.now();
        for (NotifDelivery delivery : unread) {
            delivery.setReadAt(now);
        }
        deliveryRepository.saveAll(unread);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return deliveryRepository.countByRecipientIdAndStatusAndReadAtIsNull(userId, DeliveryStatus.DELIVERED);
    }

    private InboxMessageResponse toInboxMessage(NotifDelivery delivery, Notification notification) {
        InboxMessageResponse response = new InboxMessageResponse();
        response.setDeliveryId(delivery.getId());
        response.setNotificationId(delivery.getNotificationId());
        response.setRead(delivery.getReadAt() != null);
        response.setDeliveredAt(delivery.getDeliveredAt());
        if (notification != null) {
            response.setEventType(notification.getEventType());
            response.setTitle(notification.getTitle());
            response.setBody(notification.getBody());
            response.setPriority(notification.getPriority());
        }
        return response;
    }
}
