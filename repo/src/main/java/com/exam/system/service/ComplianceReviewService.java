package com.exam.system.service;

import com.exam.system.dto.common.PageData;
import com.exam.system.dto.compliance.ComplianceReviewDetailResponse;
import com.exam.system.dto.compliance.ComplianceReviewSummaryResponse;

public interface ComplianceReviewService {

    PageData<ComplianceReviewSummaryResponse> list(String status, int page, int size);

    ComplianceReviewDetailResponse detail(Long reviewId);

    ComplianceReviewDetailResponse approve(Long reviewId, String comments);

    ComplianceReviewDetailResponse reject(Long reviewId, String comments);
}
