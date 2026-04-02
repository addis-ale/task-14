package com.exam.system.service.impl;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.compliance.ComplianceReviewDetailResponse;
import com.exam.system.dto.compliance.ComplianceReviewSummaryResponse;
import com.exam.system.entity.ComplianceReview;
import com.exam.system.entity.Notification;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.notification.ComplianceStatus;
import com.exam.system.notification.NotificationStatus;
import com.exam.system.notification.ReviewStatus;
import com.exam.system.repository.ComplianceReviewRepository;
import com.exam.system.repository.NotificationRepository;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.ComplianceReviewService;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceReviewServiceImpl implements ComplianceReviewService {

    private final ComplianceReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final PageDataBuilder pageDataBuilder;

    public ComplianceReviewServiceImpl(ComplianceReviewRepository reviewRepository,
                                       NotificationRepository notificationRepository,
                                       PageDataBuilder pageDataBuilder) {
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<ComplianceReviewSummaryResponse> list(String status, int page, int size) {
        Page<ComplianceReview> reviews;
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size));
        if (status == null || status.isBlank()) {
            reviews = reviewRepository.findAll(pageable);
        } else {
            reviews = reviewRepository.findByStatus(status, pageable);
        }
        return pageDataBuilder.from(reviews, this::toSummary, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceReviewDetailResponse detail(Long reviewId) {
        ComplianceReview review = findReview(reviewId);
        Notification notification = notificationRepository.findById(review.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Linked notification not found"));
        ComplianceReviewDetailResponse response = toDetail(review);
        response.setContentTitle(notification.getTitle());
        response.setContentBody(notification.getBody());
        return response;
    }

    @Override
    @Transactional
    public ComplianceReviewDetailResponse approve(Long reviewId, String comments) {
        ComplianceReview review = findReview(reviewId);
        Notification notification = notificationRepository.findById(review.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Linked notification not found"));

        review.setStatus(ReviewStatus.APPROVED);
        review.setComments(comments);
        review.setReviewerId(currentUserId());
        review.setDecidedAt(LocalDateTime.now());
        reviewRepository.save(review);

        notification.setComplianceStatus(ComplianceStatus.APPROVED);
        notification.setStatus(NotificationStatus.APPROVED);
        notificationRepository.save(notification);

        return toDetail(review);
    }

    @Override
    @Transactional
    public ComplianceReviewDetailResponse reject(Long reviewId, String comments) {
        ComplianceReview review = findReview(reviewId);
        Notification notification = notificationRepository.findById(review.getContentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Linked notification not found"));

        review.setStatus(ReviewStatus.REJECTED);
        review.setComments(comments);
        review.setReviewerId(currentUserId());
        review.setDecidedAt(LocalDateTime.now());
        reviewRepository.save(review);

        notification.setComplianceStatus(ComplianceStatus.REJECTED);
        notification.setStatus(NotificationStatus.REJECTED);
        notificationRepository.save(notification);

        return toDetail(review);
    }

    private ComplianceReview findReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND,
                        "Compliance review not found"));
    }

    private ComplianceReviewSummaryResponse toSummary(ComplianceReview review) {
        ComplianceReviewSummaryResponse response = new ComplianceReviewSummaryResponse();
        response.setId(review.getId());
        response.setContentType(review.getContentType());
        response.setContentId(review.getContentId());
        response.setStatus(review.getStatus());
        response.setReviewerId(review.getReviewerId());
        response.setComments(review.getComments());
        response.setDecidedAt(review.getDecidedAt());
        return response;
    }

    private ComplianceReviewDetailResponse toDetail(ComplianceReview review) {
        ComplianceReviewDetailResponse response = new ComplianceReviewDetailResponse();
        response.setId(review.getId());
        response.setContentType(review.getContentType());
        response.setContentId(review.getContentId());
        response.setStatus(review.getStatus());
        response.setReviewerId(review.getReviewerId());
        response.setComments(review.getComments());
        response.setDecidedAt(review.getDecidedAt());
        return response;
    }

    private Long currentUserId() {
        return UserContextHolder.get() == null ? null : UserContextHolder.get().getUserId();
    }
}
