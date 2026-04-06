package com.exam.system.service.impl;

import com.exam.system.dto.anticheat.AntiCheatFlagDetailResponse;
import com.exam.system.dto.anticheat.AntiCheatFlagSummaryResponse;
import com.exam.system.dto.anticheat.AntiCheatReviewRequest;
import com.exam.system.dto.common.PageData;
import com.exam.system.entity.AntiCheatFlag;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AntiCheatFlagRepository;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.AntiCheatService;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AntiCheatServiceImpl implements AntiCheatService {

    private final AntiCheatFlagRepository flagRepository;
    private final PageDataBuilder pageDataBuilder;

    public AntiCheatServiceImpl(AntiCheatFlagRepository flagRepository, PageDataBuilder pageDataBuilder) {
        this.flagRepository = flagRepository;
        this.pageDataBuilder = pageDataBuilder;
    }

    @Override
    @Transactional(readOnly = true)
    public PageData<AntiCheatFlagSummaryResponse> list(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), Math.min(100, size), Sort.by(Sort.Direction.DESC, "id"));
        Page<AntiCheatFlag> flags;
        if (status == null || status.isBlank()) {
            flags = flagRepository.findAll(pageable);
        } else {
            flags = flagRepository.findByReviewStatus(status, pageable);
        }
        return pageDataBuilder.from(flags, this::toSummary, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public AntiCheatFlagDetailResponse detail(Long flagId) {
        AntiCheatFlag flag = findFlag(flagId);
        return toDetail(flag);
    }

    @Override
    @Transactional
    public AntiCheatFlagDetailResponse review(Long flagId, AntiCheatReviewRequest request) {
        AntiCheatFlag flag = findFlag(flagId);

        if (!"PENDING".equalsIgnoreCase(flag.getReviewStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, HttpStatus.CONFLICT, "Flag has already been reviewed");
        }

        flag.setReviewStatus("REVIEWED");
        flag.setDecision(request.getDecision());
        flag.setReviewerId(currentUserId());
        flag.setDecidedAt(LocalDateTime.now());
        flagRepository.save(flag);

        return toDetail(flag);
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return flagRepository.countByReviewStatus("PENDING");
    }

    private AntiCheatFlag findFlag(Long flagId) {
        return flagRepository.findById(flagId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Anti-cheat flag not found"));
    }

    private AntiCheatFlagSummaryResponse toSummary(AntiCheatFlag flag) {
        AntiCheatFlagSummaryResponse response = new AntiCheatFlagSummaryResponse();
        response.setId(flag.getId());
        response.setUserId(flag.getUserId());
        response.setFlagType(flag.getFlagType());
        response.setReviewStatus(flag.getReviewStatus());
        response.setReviewerId(flag.getReviewerId());
        response.setDecidedAt(flag.getDecidedAt());
        return response;
    }

    private AntiCheatFlagDetailResponse toDetail(AntiCheatFlag flag) {
        AntiCheatFlagDetailResponse response = new AntiCheatFlagDetailResponse();
        response.setId(flag.getId());
        response.setUserId(flag.getUserId());
        response.setFlagType(flag.getFlagType());
        response.setDetailsJson(flag.getDetailsJson());
        response.setReviewStatus(flag.getReviewStatus());
        response.setReviewerId(flag.getReviewerId());
        response.setDecision(flag.getDecision());
        response.setDecidedAt(flag.getDecidedAt());
        return response;
    }

    private Long currentUserId() {
        return UserContextHolder.get() == null ? null : UserContextHolder.get().getUserId();
    }
}
