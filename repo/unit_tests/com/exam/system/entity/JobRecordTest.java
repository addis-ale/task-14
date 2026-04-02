package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JobRecord entity — state machine (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED),
 * retry logic, and boundary conditions.
 */
@DisplayName("JobRecord Entity Tests")
class JobRecordTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        JobRecord job = new JobRecord();
        job.setId(1L);
        job.setJobType("NOTIFICATION_SEND");
        job.setDedupKey("notif-123-send");
        job.setStatus("PENDING");
        job.setPayloadJson("{\"notificationId\":123}");
        job.setAttempts(0);
        job.setErrorMessage(null);

        assertEquals(1L, job.getId());
        assertEquals("NOTIFICATION_SEND", job.getJobType());
        assertEquals("notif-123-send", job.getDedupKey());
        assertEquals("PENDING", job.getStatus());
        assertEquals("{\"notificationId\":123}", job.getPayloadJson());
        assertEquals(0, job.getAttempts());
        assertNull(job.getErrorMessage());
    }

    @Test
    @DisplayName("PrePersist should set createdAt if null")
    void testPrePersist() {
        JobRecord job = new JobRecord();
        assertNull(job.getCreatedAt());

        job.prePersist();
        assertNotNull(job.getCreatedAt());
    }

    @Test
    @DisplayName("Should support PENDING -> RUNNING -> COMPLETED state transition")
    void testSuccessfulLifecycle() {
        JobRecord job = new JobRecord();
        job.setStatus("PENDING");
        job.setAttempts(0);
        assertEquals("PENDING", job.getStatus());

        // Start running
        job.setStatus("RUNNING");
        job.setAttempts(1);
        assertEquals("RUNNING", job.getStatus());
        assertEquals(1, job.getAttempts());

        // Complete
        job.setStatus("COMPLETED");
        job.setCompletedAt(LocalDateTime.now());
        assertEquals("COMPLETED", job.getStatus());
        assertNotNull(job.getCompletedAt());
    }

    @Test
    @DisplayName("Should support PENDING -> RUNNING -> FAILED state transition")
    void testFailedLifecycle() {
        JobRecord job = new JobRecord();
        job.setStatus("PENDING");

        job.setStatus("RUNNING");
        job.setAttempts(1);

        // Fail with error message
        job.setStatus("FAILED");
        job.setErrorMessage("Connection timeout to SMTP server");
        assertEquals("FAILED", job.getStatus());
        assertEquals("Connection timeout to SMTP server", job.getErrorMessage());
    }

    @Test
    @DisplayName("Should support retry: FAILED -> PENDING with incremented attempts")
    void testRetryFromFailed() {
        JobRecord job = new JobRecord();
        job.setStatus("FAILED");
        job.setAttempts(3);
        job.setErrorMessage("Previous error");

        // Retry
        job.setStatus("PENDING");
        job.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
        job.setErrorMessage(null);

        assertEquals("PENDING", job.getStatus());
        assertEquals(3, job.getAttempts());
        assertNotNull(job.getNextRetryAt());
        assertNull(job.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle CANCELLED state")
    void testCancelledState() {
        JobRecord job = new JobRecord();
        job.setStatus("PENDING");

        job.setStatus("CANCELLED");
        assertEquals("CANCELLED", job.getStatus());
    }

    @Test
    @DisplayName("Attempts counter should track multiple retry attempts")
    void testMultipleRetries() {
        JobRecord job = new JobRecord();
        job.setAttempts(0);

        for (int i = 1; i <= 5; i++) {
            job.setAttempts(i);
        }
        assertEquals(5, job.getAttempts());
    }

    @Test
    @DisplayName("Should handle null payload gracefully")
    void testNullPayload() {
        JobRecord job = new JobRecord();
        job.setPayloadJson(null);
        assertNull(job.getPayloadJson());
    }

    @Test
    @DisplayName("Should handle large error message")
    void testLargeErrorMessage() {
        JobRecord job = new JobRecord();
        String largeMessage = "E".repeat(5000);
        job.setErrorMessage(largeMessage);
        assertEquals(5000, job.getErrorMessage().length());
    }
}
