package com.exam.system.service;

import com.exam.system.dto.dashboard.DashboardStatsResponse;

public interface DashboardService {

    DashboardStatsResponse getStats(Long termId);
}
