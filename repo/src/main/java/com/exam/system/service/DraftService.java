package com.exam.system.service;

import com.exam.system.dto.draft.DraftResponse;

public interface DraftService {

    DraftResponse get(Long userId, String formKey);

    DraftResponse save(Long userId, String formKey, String draftJson);

    void delete(Long userId, String formKey);
}
