package com.exam.system.service.impl;

import com.exam.system.dto.draft.DraftResponse;
import com.exam.system.entity.AutoSaveDraft;
import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import com.exam.system.repository.AutoSaveDraftRepository;
import com.exam.system.service.DraftService;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DraftServiceImpl implements DraftService {

    private final AutoSaveDraftRepository draftRepository;

    public DraftServiceImpl(AutoSaveDraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DraftResponse get(Long userId, String formKey) {
        AutoSaveDraft draft = draftRepository.findByUserIdAndFormKey(userId, formKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, "Draft not found"));
        return toResponse(draft);
    }

    @Override
    @Transactional
    public DraftResponse save(Long userId, String formKey, String draftJson) {
        AutoSaveDraft draft = draftRepository.findByUserIdAndFormKey(userId, formKey)
                .orElseGet(() -> {
                    AutoSaveDraft newDraft = new AutoSaveDraft();
                    newDraft.setUserId(userId);
                    newDraft.setFormKey(formKey);
                    return newDraft;
                });
        draft.setDraftJson(draftJson);
        draft.setSavedAt(LocalDateTime.now());
        draftRepository.save(draft);
        return toResponse(draft);
    }

    @Override
    @Transactional
    public void delete(Long userId, String formKey) {
        draftRepository.deleteByUserIdAndFormKey(userId, formKey);
    }

    private DraftResponse toResponse(AutoSaveDraft draft) {
        DraftResponse response = new DraftResponse();
        response.setFormKey(draft.getFormKey());
        response.setDraftJson(draft.getDraftJson());
        response.setSavedAt(draft.getSavedAt());
        return response;
    }
}
