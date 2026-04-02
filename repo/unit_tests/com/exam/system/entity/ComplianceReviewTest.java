package com.exam.system.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComplianceReview entity — state transitions and field handling.
 */
@DisplayName("ComplianceReview Entity Tests")
class ComplianceReviewTest {

    @Test
    @DisplayName("Should set and get all fields correctly")
    void testGettersAndSetters() {
        ComplianceReview review = new ComplianceReview();
        review.setId(1L);
        review.setContentType("NOTIFICATION");
        review.setContentId(100L);
        review.setStatus("PENDING");
        review.setReviewerId(null);
        review.setComments(null);
        review.setDecidedAt(null);

        assertEquals(1L, review.getId());
        assertEquals("NOTIFICATION", review.getContentType());
        assertEquals(100L, review.getContentId());
        assertEquals("PENDING", review.getStatus());
        assertNull(review.getReviewerId());
        assertNull(review.getComments());
        assertNull(review.getDecidedAt());
    }

    @Test
    @DisplayName("Should support PENDING -> APPROVED state transition")
    void testApproveTransition() {
        ComplianceReview review = new ComplianceReview();
        review.setStatus("PENDING");

        review.setStatus("APPROVED");
        review.setReviewerId(5L);
        review.setComments("Content meets compliance standards");
        review.setDecidedAt(LocalDateTime.now());

        assertEquals("APPROVED", review.getStatus());
        assertEquals(5L, review.getReviewerId());
        assertNotNull(review.getComments());
        assertNotNull(review.getDecidedAt());
    }

    @Test
    @DisplayName("Should support PENDING -> REJECTED state transition")
    void testRejectTransition() {
        ComplianceReview review = new ComplianceReview();
        review.setStatus("PENDING");

        review.setStatus("REJECTED");
        review.setReviewerId(5L);
        review.setComments("Contains sensitive content");
        review.setDecidedAt(LocalDateTime.now());

        assertEquals("REJECTED", review.getStatus());
        assertNotNull(review.getDecidedAt());
    }

    @Test
    @DisplayName("Should handle empty comments")
    void testEmptyComments() {
        ComplianceReview review = new ComplianceReview();
        review.setComments("");
        assertEquals("", review.getComments());
    }
}
