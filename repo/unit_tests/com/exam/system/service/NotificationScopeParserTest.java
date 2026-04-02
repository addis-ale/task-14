package com.exam.system.service;

import com.exam.system.dto.notification.NotificationTargetScopeDto;
import com.exam.system.service.impl.notification.NotificationScopeParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NotificationScopeParser — JSON serialization/deserialization of target scopes.
 */
@DisplayName("NotificationScopeParser Tests")
class NotificationScopeParserTest {

    private NotificationScopeParser parser;

    @BeforeEach
    void setUp() {
        parser = new NotificationScopeParser(new ObjectMapper());
    }

    @Test
    @DisplayName("Should serialize scope to JSON")
    void testToJson() {
        NotificationTargetScopeDto scope = new NotificationTargetScopeDto();
        scope.setGradeId(1L);
        scope.setSubjectId(2L);

        String json = parser.toJson(scope);
        assertNotNull(json);
        assertTrue(json.contains("\"gradeId\""));
        assertTrue(json.contains("\"subjectId\""));
    }

    @Test
    @DisplayName("Should deserialize JSON to scope")
    void testFromJson() {
        String json = "{\"gradeId\":3,\"subjectId\":5}";
        NotificationTargetScopeDto scope = parser.fromJson(json);

        assertEquals(3L, scope.getGradeId());
        assertEquals(5L, scope.getSubjectId());
    }

    @Test
    @DisplayName("Should return empty scope for null input")
    void testFromJsonNull() {
        NotificationTargetScopeDto scope = parser.fromJson(null);
        assertNotNull(scope);
        assertNull(scope.getGradeId());
    }

    @Test
    @DisplayName("Should return empty scope for blank input")
    void testFromJsonBlank() {
        NotificationTargetScopeDto scope = parser.fromJson("   ");
        assertNotNull(scope);
        assertNull(scope.getGradeId());
    }

    @Test
    @DisplayName("Should throw on invalid JSON")
    void testFromJsonInvalid() {
        assertThrows(IllegalStateException.class,
                () -> parser.fromJson("{invalid json}"));
    }

    @Test
    @DisplayName("Should handle scope with null fields")
    void testScopeWithNulls() {
        NotificationTargetScopeDto scope = new NotificationTargetScopeDto();
        String json = parser.toJson(scope);
        assertNotNull(json);

        NotificationTargetScopeDto parsed = parser.fromJson(json);
        assertNull(parsed.getGradeId());
        assertNull(parsed.getSubjectId());
    }

    @Test
    @DisplayName("Round-trip serialization should preserve values")
    void testRoundTrip() {
        NotificationTargetScopeDto original = new NotificationTargetScopeDto();
        original.setGradeId(10L);
        original.setSubjectId(20L);

        String json = parser.toJson(original);
        NotificationTargetScopeDto restored = parser.fromJson(json);

        assertEquals(original.getGradeId(), restored.getGradeId());
        assertEquals(original.getSubjectId(), restored.getSubjectId());
    }
}
