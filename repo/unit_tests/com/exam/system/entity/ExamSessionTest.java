package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExamSession entity — field validation, defaults, and boundary values.
 */
@DisplayName("ExamSession Entity Tests")
class ExamSessionTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        ExamSession session = new ExamSession();
        session.setId(1L);
        session.setTermId(10L);
        session.setSubjectId(20L);
        session.setGradeId(30L);
        session.setDate(LocalDate.of(2026, 6, 15));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setStatus("DRAFT");
        session.setCreatedBy(100L);
        session.setVersion(1L);

        assertEquals(1L, session.getId());
        assertEquals(10L, session.getTermId());
        assertEquals(20L, session.getSubjectId());
        assertEquals(30L, session.getGradeId());
        assertEquals(LocalDate.of(2026, 6, 15), session.getDate());
        assertEquals(LocalTime.of(9, 0), session.getStartTime());
        assertEquals(LocalTime.of(11, 0), session.getEndTime());
        assertEquals("DRAFT", session.getStatus());
        assertEquals(100L, session.getCreatedBy());
        assertEquals(1L, session.getVersion());
    }

    @Test
    @DisplayName("Should allow null ID for new (unpersisted) entity")
    void testNewEntityHasNullId() {
        ExamSession session = new ExamSession();
        assertNull(session.getId());
    }

    @Test
    @DisplayName("Should handle midnight boundary times")
    void testMidnightBoundary() {
        ExamSession session = new ExamSession();
        session.setStartTime(LocalTime.of(0, 0));
        session.setEndTime(LocalTime.of(23, 59));
        assertEquals(LocalTime.MIDNIGHT, session.getStartTime());
        assertEquals(LocalTime.of(23, 59), session.getEndTime());
    }

    @Test
    @DisplayName("Should handle date at year boundary")
    void testYearBoundaryDate() {
        ExamSession session = new ExamSession();
        session.setDate(LocalDate.of(2026, 12, 31));
        assertEquals(LocalDate.of(2026, 12, 31), session.getDate());

        session.setDate(LocalDate.of(2027, 1, 1));
        assertEquals(LocalDate.of(2027, 1, 1), session.getDate());
    }

    @Test
    @DisplayName("Should allow status transitions: DRAFT -> PUBLISHED -> COMPLETED")
    void testStatusTransitions() {
        ExamSession session = new ExamSession();

        session.setStatus("DRAFT");
        assertEquals("DRAFT", session.getStatus());

        session.setStatus("PUBLISHED");
        assertEquals("PUBLISHED", session.getStatus());

        session.setStatus("COMPLETED");
        assertEquals("COMPLETED", session.getStatus());
    }

    @Test
    @DisplayName("Version field supports optimistic locking increments")
    void testVersionIncrement() {
        ExamSession session = new ExamSession();
        session.setVersion(0L);
        assertEquals(0L, session.getVersion());

        session.setVersion(1L);
        assertEquals(1L, session.getVersion());

        session.setVersion(100L);
        assertEquals(100L, session.getVersion());
    }
}
