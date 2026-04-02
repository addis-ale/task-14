package com.exam.system.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ApiResponse — the standard wrapper for all API responses.
 */
@DisplayName("ApiResponse Tests")
class ApiResponseTest {

    @Test
    @DisplayName("success() should create a response with code 200")
    void testSuccessFactory() {
        ApiResponse<String> resp = ApiResponse.success("hello");
        assertEquals(200, resp.getCode());
        assertEquals("success", resp.getMessage());
        assertEquals("hello", resp.getData());
        assertTrue(resp.getTimestamp() > 0);
    }

    @Test
    @DisplayName("successMessage() should create a response with code 200 and custom message")
    void testSuccessMessageFactory() {
        ApiResponse<Void> resp = ApiResponse.successMessage("Operation completed");
        assertEquals(200, resp.getCode());
        assertEquals("Operation completed", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    @DisplayName("Should set and get error details list")
    void testErrorDetails() {
        ApiResponse<Void> resp = new ApiResponse<>();
        resp.setCode(40001);
        resp.setMessage("Validation failed");
        resp.setErrors(java.util.List.of(new ErrorDetail("username", "must not be blank")));

        assertEquals(40001, resp.getCode());
        assertEquals(1, resp.getErrors().size());
        assertEquals("username", resp.getErrors().get(0).getField());
        assertEquals("must not be blank", resp.getErrors().get(0).getMessage());
    }

    @Test
    @DisplayName("Timestamp should be automatically set on construction")
    void testTimestampAutoSet() {
        ApiResponse<Void> resp = new ApiResponse<>();
        assertTrue(resp.getTimestamp() > 0);
    }

    @Test
    @DisplayName("Should handle null data in success response")
    void testNullData() {
        ApiResponse<Object> resp = ApiResponse.success(null);
        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
    }
}
