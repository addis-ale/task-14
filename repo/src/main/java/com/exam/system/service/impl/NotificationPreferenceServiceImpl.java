package com.exam.system.service.impl;

import com.exam.system.dto.preference.NotificationPreferenceResponse;
import com.exam.system.dto.preference.NotificationPreferenceUpdateRequest;
import com.exam.system.dto.preference.PreferenceItemDto;
import com.exam.system.entity.NotifPreference;
import com.exam.system.notification.NotificationEventType;
import com.exam.system.repository.NotifPreferenceRepository;
import com.exam.system.service.NotificationPreferenceService;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotifPreferenceRepository preferenceRepository;

    public NotificationPreferenceServiceImpl(NotifPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse get(Long userId) {
        List<NotifPreference> preferences = preferenceRepository.findByUserId(userId);
        if (preferences.isEmpty()) {
            initializeDefaultsForStudent(userId);
            preferences = preferenceRepository.findByUserId(userId);
        }
        return toResponse(preferences);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponse update(Long userId, NotificationPreferenceUpdateRequest request) {
        List<NotifPreference> existing = preferenceRepository.findByUserId(userId);
        if (existing.isEmpty()) {
            initializeDefaultsForStudent(userId);
            existing = preferenceRepository.findByUserId(userId);
        }

        Map<String, NotifPreference> byEventType = existing.stream()
                .collect(Collectors.toMap(NotifPreference::getEventType, p -> p));

        if (request.getPreferences() != null) {
            for (PreferenceItemDto item : request.getPreferences()) {
                NotifPreference pref = byEventType.computeIfAbsent(item.getEventType(), event -> {
                    NotifPreference created = new NotifPreference();
                    created.setUserId(userId);
                    created.setEventType(event);
                    created.setEnabled(false);
                    return created;
                });
                pref.setEnabled(item.isEnabled());
            }
        }

        LocalTime dndStart = request.getDndStart();
        LocalTime dndEnd = request.getDndEnd();
        if (dndStart != null || dndEnd != null) {
            for (NotifPreference preference : byEventType.values()) {
                if (dndStart != null) {
                    preference.setDndStart(dndStart);
                }
                if (dndEnd != null) {
                    preference.setDndEnd(dndEnd);
                }
            }
        }

        preferenceRepository.saveAll(byEventType.values());
        return toResponse(new ArrayList<>(byEventType.values()));
    }

    @Override
    @Transactional
    public void initializeDefaultsForStudent(Long userId) {
        if (!preferenceRepository.findByUserId(userId).isEmpty()) {
            return;
        }
        List<NotifPreference> defaults = new ArrayList<>();
        defaults.add(build(userId, NotificationEventType.SCHEDULE_CHANGE, true));
        defaults.add(build(userId, NotificationEventType.REVIEW_OUTCOME, true));
        defaults.add(build(userId, NotificationEventType.CHECKIN_REMINDER, true));
        defaults.add(build(userId, NotificationEventType.GENERAL_ANNOUNCEMENT, false));
        preferenceRepository.saveAll(defaults);
    }

    private NotifPreference build(Long userId, String eventType, boolean enabled) {
        NotifPreference preference = new NotifPreference();
        preference.setUserId(userId);
        preference.setEventType(eventType);
        preference.setEnabled(enabled);
        return preference;
    }

    private NotificationPreferenceResponse toResponse(List<NotifPreference> preferences) {
        NotificationPreferenceResponse response = new NotificationPreferenceResponse();
        response.setPreferences(preferences.stream()
                .sorted(Comparator.comparing(NotifPreference::getEventType))
                .map(pref -> {
                    PreferenceItemDto item = new PreferenceItemDto();
                    item.setEventType(pref.getEventType());
                    item.setEnabled(pref.isEnabled());
                    return item;
                })
                .toList());

        NotifPreference first = preferences.isEmpty() ? null : preferences.get(0);
        if (first != null) {
            response.setDndStart(first.getDndStart());
            response.setDndEnd(first.getDndEnd());
        }
        return response;
    }
}
