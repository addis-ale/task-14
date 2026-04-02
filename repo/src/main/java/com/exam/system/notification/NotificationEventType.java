package com.exam.system.notification;

import java.util.Map;
import java.util.Set;

public final class NotificationEventType {

    public static final String SCHEDULE_CHANGE = "SCHEDULE_CHANGE";
    public static final String REVIEW_OUTCOME = "REVIEW_OUTCOME";
    public static final String CHECKIN_REMINDER = "CHECKIN_REMINDER";
    public static final String GENERAL_ANNOUNCEMENT = "GENERAL_ANNOUNCEMENT";

    public static final Set<String> SUPPORTED = Set.of(
            SCHEDULE_CHANGE,
            REVIEW_OUTCOME,
            CHECKIN_REMINDER,
            GENERAL_ANNOUNCEMENT
    );

    public static final Map<String, String> PRIORITY_BY_EVENT = Map.of(
            SCHEDULE_CHANGE, "HIGH",
            REVIEW_OUTCOME, "HIGH",
            CHECKIN_REMINDER, "MEDIUM",
            GENERAL_ANNOUNCEMENT, "LOW"
    );

    private NotificationEventType() {
    }
}
