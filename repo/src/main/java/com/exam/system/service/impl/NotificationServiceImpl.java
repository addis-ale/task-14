package com.exam.system.service.impl;

import com.exam.system.config.InputSanitizer;
import com.exam.system.dto.common.PageData;
import com.exam.system.dto.notification.DeliveryResponse;
import com.exam.system.dto.notification.DeliveryStatsDto;
import com.exam.system.dto.notification.NotificationCreateRequest;
import com.exam.system.dto.notification.NotificationDetailResponse;
import com.exam.system.dto.notification.NotificationSummaryResponse;
import com.exam.system.dto.notification.NotificationTargetScopeDto;
import com.exam.system.entity.ComplianceReview;
import com.exam.system.entity.NotifDelivery;
import com.exam.system.entity.Notification;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.notification.ComplianceStatus;
import com.exam.system.notification.DeliveryStatus;
import com.exam.system.notification.NotificationEventType;
import com.exam.system.notification.NotificationPublishedEvent;
import com.exam.system.notification.NotificationStatus;
import com.exam.system.notification.ReviewStatus;
import com.exam.system.repository.ComplianceReviewRepository;
import com.exam.system.repository.NotifDeliveryRepository;
import com.exam.system.repository.NotificationRepository;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.NotificationService;
import com.exam.system.service.impl.notification.ComplianceContentScanner;
import com.exam.system.service.impl.notification.NotificationDeliveryPipeline;
import com.exam.system.service.impl.notification.NotificationScopeParser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotifDeliveryRepository deliveryRepository;
    private final ComplianceReviewRepository complianceReviewRepository;
    private final PageDataBuilder pageDataBuilder;
    private final NotificationScopeParser scopeParser;
    private final ComplianceContentScanner contentScanner;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationDeliveryPipeline deliveryPipeline;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotifDeliveryRepository deliveryRepository,
                                   ComplianceReviewRepository complianceReviewRepository,
                                   PageDataBuilder pageDataBuilder,
                                   NotificationScopeParser scopeParser,
                                   ComplianceContentScanner contentScanner,
                                   ApplicationEventPublisher eventPublisher,
                                   NotificationDeliveryPipeline deliveryPipeline) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.complianceReviewRepository = complianceReviewRepository;
        this.pageDataBuilder = pageDataBuilder;
        this.scopeParser = scopeParser;
        this.contentScanner = contentScanner;
        this.eventPublisher = eventPublisher;
        this.deliveryPipeline = deliveryPipeline;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<NotificationSummaryResponse> list(String eventType, String status, int page, int size) {
        Specification<Notification> spec = Specification.where(null);
        if (eventType != null && !eventType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        Page<Notification> notifications = notificationRepository.findAll(spec,
                PageRequest.of(Math.max(0, page - 1), Math.min(size, 100)));
        return pageDataBuilder.from(notifications, this::toSummary, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDetailResponse detail(Long notificationId) {
        Notification notification = findById(notificationId);
        NotificationDetailResponse response = toDetail(notification);
        response.setDeliveryStats(buildStats(notification.getId()));
        return response;
    }

    @Override
    @Transactional
    public NotificationSummaryResponse create(NotificationCreateRequest request) {
        validateRequest(request);
        Notification notification = new Notification();
        notification.setEventType(request.getEventType());
        notification.setTitle(InputSanitizer.sanitize(request.getTitle()));
        notification.setBody(InputSanitizer.sanitize(request.getBody()));
        notification.setPriority(request.getPriority());
        notification.setTargetScope(scopeParser.toJson(request.getTargetScope()));
        notification.setStatus(NotificationStatus.DRAFT);
        notification.setComplianceStatus(null);
        notification.setPublishedBy(UserContextHolder.get() == null ? null : UserContextHolder.get().getUserId());
        return toSummary(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void submitReview(Long notificationId) {
        Notification notification = findById(notificationId);
        if (!NotificationStatus.DRAFT.equals(notification.getStatus()) && !NotificationStatus.REJECTED.equals(notification.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Only DRAFT or REJECTED notifications can be submitted for review");
        }

        notification.setStatus(NotificationStatus.PENDING_REVIEW);
        notification.setComplianceStatus(ComplianceStatus.PENDING);
        notificationRepository.save(notification);

        ComplianceReview review = complianceReviewRepository
                .findByContentTypeAndContentId("NOTIFICATION", notificationId)
                .orElseGet(() -> {
                    ComplianceReview created = new ComplianceReview();
                    created.setContentType("NOTIFICATION");
                    created.setContentId(notificationId);
                    return created;
                });
        review.setStatus(ReviewStatus.PENDING);

        List<String> findings = contentScanner.scan(notification.getTitle(), notification.getBody());
        if (findings.isEmpty()) {
            review.setComments("AUTO_CHECK: no blocking findings");
        } else {
            review.setComments("AUTO_CHECK: " + String.join(" | ", findings));
        }
        review.setReviewerId(null);
        review.setDecidedAt(null);
        complianceReviewRepository.save(review);
    }

    @Override
    @Transactional
    public void publish(Long notificationId) {
        Notification notification = findById(notificationId);
        if (!ComplianceStatus.APPROVED.equals(notification.getComplianceStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Notification must be approved before publish");
        }
        notification.setStatus(NotificationStatus.SENDING);
        notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationPublishedEvent(notification.getId()));
    }

    @Override
    @Transactional
    public void delete(Long notificationId) {
        Notification notification = findById(notificationId);
        if (!(NotificationStatus.DRAFT.equals(notification.getStatus())
                || NotificationStatus.REJECTED.equals(notification.getStatus()))) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Only DRAFT or REJECTED notifications can be deleted");
        }
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<DeliveryResponse> listDeliveries(Long notificationId, String status, int page, int size) {
        findById(notificationId);
        Page<NotifDelivery> result;
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size));
        if (status == null || status.isBlank()) {
            result = deliveryRepository.findByNotificationId(notificationId, pageable);
        } else {
            result = deliveryRepository.findByNotificationIdAndStatus(notificationId, status, pageable);
        }
        return pageDataBuilder.from(result, this::toDeliveryResponse, page, size);
    }

    @Override
    @Transactional
    public void retryDelivery(Long notificationId, Long deliveryId) {
        findById(notificationId);
        NotifDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Delivery not found"));
        if (!delivery.getNotificationId().equals(notificationId)) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT,
                    "Delivery does not belong to notification");
        }
        deliveryPipeline.retryDelivery(deliveryId);
    }

    private void validateRequest(NotificationCreateRequest request) {
        if (!NotificationEventType.SUPPORTED.contains(request.getEventType())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST, "Unsupported event type");
        }
        String expectedPriority = NotificationEventType.PRIORITY_BY_EVENT.get(request.getEventType());
        if (!expectedPriority.equalsIgnoreCase(request.getPriority())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Priority does not match event type requirement");
        }
    }

    private Notification findById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private NotificationSummaryResponse toSummary(Notification notification) {
        NotificationSummaryResponse response = new NotificationSummaryResponse();
        response.setId(notification.getId());
        response.setEventType(notification.getEventType());
        response.setTitle(notification.getTitle());
        response.setPriority(notification.getPriority());
        response.setStatus(notification.getStatus());
        response.setComplianceStatus(notification.getComplianceStatus());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    private NotificationDetailResponse toDetail(Notification notification) {
        NotificationDetailResponse response = new NotificationDetailResponse();
        response.setId(notification.getId());
        response.setEventType(notification.getEventType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setPriority(notification.getPriority());
        response.setStatus(notification.getStatus());
        response.setComplianceStatus(notification.getComplianceStatus());
        response.setCreatedAt(notification.getCreatedAt());
        NotificationTargetScopeDto scope = scopeParser.fromJson(notification.getTargetScope());
        response.setTargetScope(scope);
        return response;
    }

    private DeliveryStatsDto buildStats(Long notificationId) {
        DeliveryStatsDto stats = new DeliveryStatsDto();
        stats.setTotal(deliveryRepository.countByNotificationId(notificationId));
        stats.setDelivered(deliveryRepository.countByNotificationIdAndStatus(notificationId, DeliveryStatus.DELIVERED));
        long failed = deliveryRepository.countByNotificationIdAndStatus(notificationId, DeliveryStatus.FAILED);
        stats.setFailed(failed);
        long pending = deliveryRepository.countByNotificationIdAndStatus(notificationId, DeliveryStatus.PENDING)
                + deliveryRepository.countByNotificationIdAndStatus(notificationId, DeliveryStatus.HELD_DND)
                + deliveryRepository.countByNotificationIdAndStatus(notificationId, DeliveryStatus.SENDING);
        stats.setPending(pending);
        return stats;
    }

    private DeliveryResponse toDeliveryResponse(NotifDelivery delivery) {
        DeliveryResponse response = new DeliveryResponse();
        response.setId(delivery.getId());
        response.setRecipientId(delivery.getRecipientId());
        response.setChannel(delivery.getChannel().name());
        response.setStatus(delivery.getStatus());
        response.setAttempts(delivery.getAttempts());
        response.setLastAttemptAt(delivery.getLastAttemptAt());
        response.setDeliveredAt(delivery.getDeliveredAt());
        return response;
    }
}
