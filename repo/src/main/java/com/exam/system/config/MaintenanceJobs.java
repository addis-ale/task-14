package com.exam.system.config;

import com.exam.system.repository.SysSessionRepository;
import com.exam.system.service.RequestNonceService;
import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaintenanceJobs {

    private final RequestNonceService requestNonceService;
    private final SysSessionRepository sessionRepository;

    public MaintenanceJobs(RequestNonceService requestNonceService,
                           SysSessionRepository sessionRepository) {
        this.requestNonceService = requestNonceService;
        this.sessionRepository = sessionRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanupExpiredSecurityArtifacts() {
        requestNonceService.cleanupExpired();
        sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
