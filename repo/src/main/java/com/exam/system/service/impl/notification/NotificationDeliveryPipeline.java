package com.exam.system.service.impl.notification;

import com.exam.system.dto.notification.NotificationTargetScopeDto;
import com.exam.system.entity.JobRecord;
import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.NotifPreference;
import com.exam.system.entity.Notification;
import com.exam.system.entity.NotificationChannel;
import com.exam.system.entity.SysUser;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.notification.DeliveryStatus;
import com.exam.system.notification.NotificationPublishedEvent;
import com.exam.system.notification.NotificationStatus;
import com.exam.system.job.JobQueueService;
import com.exam.system.job.JobType;
import com.exam.system.repository.NotifDeliveryRepository;
import com.exam.system.repository.NotifPreferenceRepository;
import com.exam.system.repository.NotificationRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.service.impl.delivery.DeliveryAttemptResult;
import com.exam.system.service.impl.delivery.NotificationDeliveryStrategy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationDeliveryPipeline {

    private final NotificationRepository notificationRepository;
    private final NotifDeliveryRepository deliveryRepository;
    private final NotifPreferenceRepository preferenceRepository;
    private final SysUserRepository userRepository;
    private final JobQueueService jobQueueService;
    private final NotificationTargetResolver targetResolver;
    private final NotificationScopeParser scopeParser;
    private final Map<NotificationChannel, NotificationDeliveryStrategy> strategies;

    public NotificationDeliveryPipeline(NotificationRepository notificationRepository,
                                        NotifDeliveryRepository deliveryRepository,
                                        NotifPreferenceRepository preferenceRepository,
                                        SysUserRepository userRepository,
                                        JobQueueService jobQueueService,
                                        NotificationTargetResolver targetResolver,
                                        NotificationScopeParser scopeParser,
                                        List<NotificationDeliveryStrategy> strategyList) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.jobQueueService = jobQueueService;
        this.targetResolver = targetResolver;
        this.scopeParser = scopeParser;
        this.strategies = new HashMap<>();
        for (NotificationDeliveryStrategy strategy : strategyList) {
            this.strategies.put(strategy.channel(), strategy);
        }
    }

    @EventListener
    @Transactional
    public void onPublished(NotificationPublishedEvent event) {
        Notification notification = notificationRepository.findById(event.getNotificationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));
        jobQueueService.enqueue(
                JobType.NOTIFICATION_SEND,
                "send-notification-" + notification.getId(),
                "{\"notificationId\":" + notification.getId() + "}");
    }

    @Transactional
    public void retryDelivery(Long deliveryId) {
        NotifDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Delivery not found"));
        attemptDelivery(delivery, true);
    }

    @Transactional
    public void executeNotificationSend(JobRecord jobRecord) {
        Long notificationId = extractLong(jobRecord.getPayloadJson(), "notificationId");
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));
        createRecipientDeliveries(notification);
        notification.setStatus(NotificationStatus.SENT);
        notificationRepository.save(notification);
    }

    @Transactional
    public void executeRetryDelivery(JobRecord jobRecord) {
        Long deliveryId = extractLong(jobRecord.getPayloadJson(), "deliveryId");
        NotifDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Delivery not found"));
        attemptDelivery(delivery, false);
    }

    @Transactional
    public void executeDndRelease(JobRecord jobRecord) {
        List<NotifDelivery> held = deliveryRepository.findByStatus(DeliveryStatus.HELD_DND);
        for (NotifDelivery delivery : held) {
            Notification notification = notificationRepository.findById(delivery.getNotificationId()).orElse(null);
            SysUser user = userRepository.findById(delivery.getRecipientId()).orElse(null);
            if (notification == null || user == null) {
                continue;
            }
            if (!isWithinDnd(user.getId(), notification.getEventType())) {
                attemptDelivery(delivery, false);
            }
        }
    }

    private void createRecipientDeliveries(Notification notification) {
        NotificationTargetScopeDto scope = scopeParser.fromJson(notification.getTargetScope());
        Set<Long> recipients = targetResolver.resolveStudentRecipientIds(scope);

        for (Long recipientId : recipients) {
            NotifDelivery delivery = new NotifDelivery();
            delivery.setNotificationId(notification.getId());
            delivery.setRecipientId(recipientId);
            delivery.setChannel(NotificationChannel.APP);
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setAttempts(0);
            deliveryRepository.save(delivery);

            attemptDelivery(delivery, false);
        }
    }

    private void attemptDelivery(NotifDelivery delivery, boolean manualRetry) {
        Notification notification = notificationRepository.findById(delivery.getNotificationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));
        SysUser user = userRepository.findById(delivery.getRecipientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Recipient not found"));

        Optional<NotifPreference> preference = preferenceRepository.findByUserIdAndEventType(user.getId(), notification.getEventType());
        if (preference.isPresent() && !preference.get().isEnabled()) {
            delivery.setStatus(DeliveryStatus.FAILED);
            deliveryRepository.save(delivery);
            return;
        }

        if (!"HIGH".equalsIgnoreCase(notification.getPriority()) && isWithinDnd(user.getId(), notification.getEventType())) {
            delivery.setStatus(DeliveryStatus.HELD_DND);
            deliveryRepository.save(delivery);
            return;
        }

        delivery.setStatus(DeliveryStatus.SENDING);
        delivery.setLastAttemptAt(LocalDateTime.now());
        delivery.setAttempts(delivery.getAttempts() + 1);

        NotificationDeliveryStrategy wechat = strategies.get(NotificationChannel.WECHAT);
        NotificationDeliveryStrategy inApp = strategies.get(NotificationChannel.APP);

        DeliveryAttemptResult result;
        NotificationChannel usedChannel;
        if (wechat != null && wechat.supports(user)) {
            result = wechat.deliver(notification, delivery, user);
            usedChannel = NotificationChannel.WECHAT;
            if (!result.isSuccess() && result.isChannelUnavailable() && inApp != null) {
                result = inApp.deliver(notification, delivery, user);
                usedChannel = NotificationChannel.APP;
            }
        } else {
            result = inApp.deliver(notification, delivery, user);
            usedChannel = NotificationChannel.APP;
        }

        delivery.setChannel(usedChannel);
        if (result.isSuccess()) {
            delivery.setStatus(DeliveryStatus.DELIVERED);
            delivery.setDeliveredAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            return;
        }

        if (manualRetry) {
            delivery.setStatus(DeliveryStatus.FAILED);
            deliveryRepository.save(delivery);
            return;
        }

        if (delivery.getAttempts() >= 3) {
            delivery.setStatus(DeliveryStatus.FAILED);
            deliveryRepository.save(delivery);
            return;
        }

        delivery.setStatus(DeliveryStatus.PENDING);
        deliveryRepository.save(delivery);

        LocalDateTime nextRetry = LocalDateTime.now().plusMinutes(backoffMinutes(delivery.getAttempts()));
        jobQueueService.enqueue(
                JobType.RETRY_DELIVERY,
                "retry-delivery-" + delivery.getId() + "-" + delivery.getAttempts(),
                "{\"deliveryId\":" + delivery.getId() + "}",
                nextRetry);
    }

    private Long extractLong(String payload, String key) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Missing payload");
        }
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(payload);
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid payload for key " + key);
        }
        return Long.parseLong(matcher.group(1));
    }

    private int backoffMinutes(int attempt) {
        return switch (attempt) {
            case 1 -> 1;
            case 2 -> 5;
            default -> 15;
        };
    }

    private boolean isWithinDnd(Long userId, String eventType) {
        Optional<NotifPreference> pref = preferenceRepository.findByUserIdAndEventType(userId, eventType);
        if (pref.isEmpty() || pref.get().getDndStart() == null || pref.get().getDndEnd() == null) {
            return false;
        }
        LocalTime now = LocalTime.now();
        LocalTime start = pref.get().getDndStart();
        LocalTime end = pref.get().getDndEnd();

        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }
}
