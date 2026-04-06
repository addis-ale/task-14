package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuditLog entity.
 */
@DisplayName("AuditLog Entity Tests")
class AuditLogTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setUserId(10L);
        log.setAction("CREATE");
        log.setResource("ExamSession");
        log.setResourceId("42");
        log.setIpAddress("192.168.1.100");
        log.setDetailsJson("{\"field\":\"value\"}");

        assertEquals(1L, log.getId());
        assertEquals(10L, log.getUserId());
        assertEquals("CREATE", log.getAction());
        assertEquals("ExamSession", log.getResource());
        assertEquals("42", log.getResourceId());
        assertEquals("192.168.1.100", log.getIpAddress());
        assertEquals("{\"field\":\"value\"}", log.getDetailsJson());
    }

    @Test
    @DisplayName("PrePersist should set timestamp if null")
    void testPrePersist() {
        AuditLog log = new AuditLog();
        assertNull(log.getTimestamp());

        log.prePersist();
        assertNotNull(log.getTimestamp());
    }

    @Test
    @DisplayName("PrePersist should not overwrite existing timestamp")
    void testPrePersistDoesNotOverwrite() {
        AuditLog log = new AuditLog();
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 1, 0, 0);
        log.setTimestamp(fixed);

        log.prePersist();
        assertEquals(fixed, log.getTimestamp());
    }

    @Test
    @DisplayName("Should handle null userId for system actions")
    void testNullUserId() {
        AuditLog log = new AuditLog();
        log.setUserId(null);
        assertNull(log.getUserId());
    }

    @Test
    @DisplayName("Should handle null IP address")
    void testNullIpAddress() {
        AuditLog log = new AuditLog();
        log.setIpAddress(null);
        assertNull(log.getIpAddress());
    }
}
