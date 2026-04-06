package com.exam.system.service;

import com.exam.system.dto.draft.DraftResponse;
import com.exam.system.entity.AutoSaveDraft;
import com.exam.system.exception.BusinessException;
import com.exam.system.repository.AutoSaveDraftRepository;
import com.exam.system.service.impl.DraftServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

    @Mock private AutoSaveDraftRepository draftRepository;

    private DraftServiceImpl draftService;

    @BeforeEach
    void setUp() {
        draftService = new DraftServiceImpl(draftRepository);
    }

    @Test
    @DisplayName("Get draft returns saved draft")
    void getDraftReturnsSavedDraft() {
        AutoSaveDraft draft = createDraft(1L, 10L, "session-create", "{\"name\":\"test\"}");
        when(draftRepository.findByUserIdAndFormKey(10L, "session-create")).thenReturn(Optional.of(draft));

        DraftResponse response = draftService.get(10L, "session-create");

        assertThat(response.getFormKey()).isEqualTo("session-create");
        assertThat(response.getDraftJson()).isEqualTo("{\"name\":\"test\"}");
    }

    @Test
    @DisplayName("Get draft throws when not found")
    void getDraftThrowsWhenNotFound() {
        when(draftRepository.findByUserIdAndFormKey(10L, "nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> draftService.get(10L, "nonexistent"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Save creates new draft when none exists")
    void saveCreatesNewDraft() {
        when(draftRepository.findByUserIdAndFormKey(10L, "roster-import")).thenReturn(Optional.empty());
        when(draftRepository.save(any(AutoSaveDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = draftService.save(10L, "roster-import", "{\"data\":\"new\"}");

        assertThat(response.getFormKey()).isEqualTo("roster-import");
        assertThat(response.getDraftJson()).isEqualTo("{\"data\":\"new\"}");
        verify(draftRepository).save(any(AutoSaveDraft.class));
    }

    @Test
    @DisplayName("Save updates existing draft")
    void saveUpdatesExistingDraft() {
        AutoSaveDraft existing = createDraft(1L, 10L, "session-create", "{\"old\":\"data\"}");
        when(draftRepository.findByUserIdAndFormKey(10L, "session-create")).thenReturn(Optional.of(existing));
        when(draftRepository.save(any(AutoSaveDraft.class))).thenAnswer(inv -> inv.getArgument(0));

        DraftResponse response = draftService.save(10L, "session-create", "{\"new\":\"data\"}");

        assertThat(response.getDraftJson()).isEqualTo("{\"new\":\"data\"}");
    }

    @Test
    @DisplayName("Delete calls repository")
    void deleteCallsRepository() {
        draftService.delete(10L, "session-create");
        verify(draftRepository).deleteByUserIdAndFormKey(10L, "session-create");
    }

    private AutoSaveDraft createDraft(Long id, Long userId, String formKey, String json) {
        AutoSaveDraft draft = new AutoSaveDraft();
        draft.setId(id);
        draft.setUserId(userId);
        draft.setFormKey(formKey);
        draft.setDraftJson(json);
        draft.setSavedAt(LocalDateTime.now());
        return draft;
    }
}
