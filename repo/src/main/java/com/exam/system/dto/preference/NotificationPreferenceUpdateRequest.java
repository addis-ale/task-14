package com.exam.system.dto.preference;

import jakarta.validation.Valid;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationPreferenceUpdateRequest {

    @Valid
    private List<PreferenceItemDto> preferences = new ArrayList<>();

    private LocalTime dndStart;
    private LocalTime dndEnd;

    public List<PreferenceItemDto> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<PreferenceItemDto> preferences) {
        this.preferences = preferences;
    }

    public LocalTime getDndStart() {
        return dndStart;
    }

    public void setDndStart(LocalTime dndStart) {
        this.dndStart = dndStart;
    }

    public LocalTime getDndEnd() {
        return dndEnd;
    }

    public void setDndEnd(LocalTime dndEnd) {
        this.dndEnd = dndEnd;
    }
}
