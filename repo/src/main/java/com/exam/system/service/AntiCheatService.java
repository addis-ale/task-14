package com.exam.system.service;

import com.exam.system.dto.anticheat.AntiCheatFlagDetailResponse;
import com.exam.system.dto.anticheat.AntiCheatFlagSummaryResponse;
import com.exam.system.dto.anticheat.AntiCheatReviewRequest;
import com.exam.system.dto.common.PageData;

public interface AntiCheatService {

    PageData<AntiCheatFlagSummaryResponse> list(String status, int page, int size);

    AntiCheatFlagDetailResponse detail(Long flagId);

    AntiCheatFlagDetailResponse review(Long flagId, AntiCheatReviewRequest request);

    long countPending();
}
