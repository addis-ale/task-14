package com.exam.system.service;

import com.exam.system.dto.anticheat.AntiCheatFlagDetailResponse;
import com.exam.system.dto.anticheat.AntiCheatReviewRequest;
import com.exam.system.entity.AntiCheatFlag;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.AntiCheatFlagRepository;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.impl.AntiCheatServiceImpl;
import com.exam.system.service.impl.PageDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AntiCheatServiceTest {

    @Mock private AntiCheatFlagRepository flagRepository;

    private AntiCheatServiceImpl antiCheatService;

    @BeforeEach
    void setUp() {
        antiCheatService = new AntiCheatServiceImpl(flagRepository, new PageDataBuilder());
        UserContext context = new UserContext();
        context.setUserId(1L);
        context.setUsername("admin");
        context.setActiveRole("ADMIN");
        UserContextHolder.set(context);
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("Review sets decision and reviewer")
    void reviewSetsDecisionAndReviewer() {
        AntiCheatFlag flag = createFlag(1L, "PENDING");
        when(flagRepository.findById(1L)).thenReturn(Optional.of(flag));
        when(flagRepository.save(any(AntiCheatFlag.class))).thenAnswer(inv -> inv.getArgument(0));

        AntiCheatReviewRequest request = new AntiCheatReviewRequest();
        request.setDecision("DISMISSED");
        request.setComments("False positive");

        AntiCheatFlagDetailResponse response = antiCheatService.review(1L, request);

        assertThat(response.getReviewStatus()).isEqualTo("REVIEWED");
        assertThat(response.getDecision()).isEqualTo("DISMISSED");
        assertThat(response.getReviewerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Review rejects already-reviewed flag")
    void reviewRejectsAlreadyReviewedFlag() {
        AntiCheatFlag flag = createFlag(1L, "REVIEWED");
        when(flagRepository.findById(1L)).thenReturn(Optional.of(flag));

        AntiCheatReviewRequest request = new AntiCheatReviewRequest();
        request.setDecision("DISMISSED");

        assertThatThrownBy(() -> antiCheatService.review(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Detail throws when flag not found")
    void detailThrowsWhenNotFound() {
        when(flagRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> antiCheatService.detail(999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Count pending returns repository count")
    void countPendingReturnsRepositoryCount() {
        when(flagRepository.countByReviewStatus("PENDING")).thenReturn(5L);

        assertThat(antiCheatService.countPending()).isEqualTo(5L);
    }

    private AntiCheatFlag createFlag(Long id, String status) {
        AntiCheatFlag flag = new AntiCheatFlag();
        flag.setId(id);
        flag.setUserId(100L);
        flag.setFlagType("ACTIVITY_BURST");
        flag.setDetailsJson("{\"count\":50}");
        flag.setReviewStatus(status);
        return flag;
    }
}
