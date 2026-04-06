package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Notification entity — state transitions, field validation, lifecycle.
 */
@DisplayName("Notification Entity Tests")
class NotificationTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setEventType("EXAM_SCHEDULE_PUBLISHED");
        notif.setTitle("期末考试安排");
        notif.setBody("请注意考试时间安排");
        notif.setPriority("HIGH");
        notif.setStatus("DRAFT");
        notif.setTargetScope("{\"gradeId\":1}");
        notif.setPublishedBy(10L);
        notif.setComplianceStatus("PENDING");

        assertEquals(1L, notif.getId());
        assertEquals("EXAM_SCHEDULE_PUBLISHED", notif.getEventType());
        assertEquals("期末考试安排", notif.getTitle());
        assertEquals("请注意考试时间安排", notif.getBody());
        assertEquals("HIGH", notif.getPriority());
        assertEquals("DRAFT", notif.getStatus());
        assertEquals("{\"gradeId\":1}", notif.getTargetScope());
        assertEquals(10L, notif.getPublishedBy());
        assertEquals("PENDING", notif.getComplianceStatus());
    }

    @Test
    @DisplayName("PrePersist should set createdAt if null")
    void testPrePersist() {
        Notification notif = new Notification();
        assertNull(notif.getCreatedAt());

        notif.prePersist();
        assertNotNull(notif.getCreatedAt());
    }

    @Test
    @DisplayName("PrePersist should NOT overwrite existing createdAt")
    void testPrePersistDoesNotOverwrite() {
        Notification notif = new Notification();
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 0, 0);
        notif.setCreatedAt(fixed);

        notif.prePersist();
        assertEquals(fixed, notif.getCreatedAt());
    }

    @Test
    @DisplayName("Should support full notification lifecycle status transitions")
    void testLifecycleTransitions() {
        Notification notif = new Notification();

        // Create as DRAFT
        notif.setStatus("DRAFT");
        assertEquals("DRAFT", notif.getStatus());

        // Submit for review
        notif.setStatus("PENDING_REVIEW");
        assertEquals("PENDING_REVIEW", notif.getStatus());

        // Compliance approved
        notif.setStatus("APPROVED");
        notif.setComplianceStatus("APPROVED");
        assertEquals("APPROVED", notif.getStatus());

        // Publish -> SENDING
        notif.setStatus("SENDING");
        assertEquals("SENDING", notif.getStatus());

        // Complete
        notif.setStatus("SENT");
        assertEquals("SENT", notif.getStatus());
    }

    @Test
    @DisplayName("Should handle REJECTED compliance status")
    void testRejectedStatus() {
        Notification notif = new Notification();
        notif.setStatus("REJECTED");
        notif.setComplianceStatus("REJECTED");

        assertEquals("REJECTED", notif.getStatus());
        assertEquals("REJECTED", notif.getComplianceStatus());
    }

    @Test
    @DisplayName("Should handle all priority levels")
    void testPriorityLevels() {
        Notification notif = new Notification();

        for (String priority : new String[]{"HIGH", "MEDIUM", "LOW"}) {
            notif.setPriority(priority);
            assertEquals(priority, notif.getPriority());
        }
    }
}
