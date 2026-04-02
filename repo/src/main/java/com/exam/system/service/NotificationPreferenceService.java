package com.exam.system.service;

import com.exam.system.dto.preference.NotificationPreferenceResponse;
import com.exam.system.dto.preference.NotificationPreferenceUpdateRequest;

public interface NotificationPreferenceService {

    NotificationPreferenceResponse get(Long userId);

    NotificationPreferenceResponse update(Long userId, NotificationPreferenceUpdateRequest request);

    void initializeDefaultsForStudent(Long userId);
}
