package com.exam.system.service.impl;

import com.exam.system.dto.dashboard.DashboardStatsResponse;
import com.exam.system.repository.AntiCheatFlagRepository;
import com.exam.system.repository.ComplianceReviewRepository;
import com.exam.system.repository.ExamSessionRepository;
import com.exam.system.repository.JobRecordRepository;
import com.exam.system.repository.SysUserRepository;
import com.exam.system.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ExamSessionRepository sessionRepository;
    private final SysUserRepository userRepository;
    private final ComplianceReviewRepository reviewRepository;
    private final AntiCheatFlagRepository antiCheatFlagRepository;
    private final JobRecordRepository jobRecordRepository;

    public DashboardServiceImpl(ExamSessionRepository sessionRepository,
                                SysUserRepository userRepository,
                                ComplianceReviewRepository reviewRepository,
                                AntiCheatFlagRepository antiCheatFlagRepository,
                                JobRecordRepository jobRecordRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.antiCheatFlagRepository = antiCheatFlagRepository;
        this.jobRecordRepository = jobRecordRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long termId) {
        DashboardStatsResponse stats = new DashboardStatsResponse();

        if (termId != null) {
            stats.setTotalSessions(sessionRepository.countByTermId(termId));
        } else {
            stats.setTotalSessions(sessionRepository.count());
        }

        stats.setTotalStudents(userRepository.findActiveStudents().size());
        stats.setPendingReviews(reviewRepository.findByStatus("PENDING", PageRequest.of(0, 1)).getTotalElements());
        stats.setPendingAntiCheatFlags(antiCheatFlagRepository.countByReviewStatus("PENDING"));

        Specification<com.exam.system.entity.JobRecord> activeSpec = (root, query, cb) ->
                cb.equal(root.get("status"), "RUNNING");
        stats.setActiveJobs(jobRecordRepository.count(activeSpec));

        Specification<com.exam.system.entity.JobRecord> failedSpec = (root, query, cb) ->
                cb.equal(root.get("status"), "FAILED");
        stats.setFailedJobs(jobRecordRepository.count(failedSpec));

        return stats;
    }
}
