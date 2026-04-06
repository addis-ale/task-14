package com.exam.system.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BusinessException and ErrorCode — exception handling mechanism.
 */
@DisplayName("BusinessException and ErrorCode Tests")
class BusinessExceptionTest {

    @Test
    @DisplayName("Should create BusinessException with all fields")
    void testConstructor() {
        BusinessException ex = new BusinessException(
                ErrorCode.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Invalid input data");

        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid input data", ex.getMessage());
    }

    @Test
    @DisplayName("Should map each ErrorCode to its numeric code")
    void testErrorCodes() {
        assertEquals(40001, ErrorCode.VALIDATION_FAILED.getCode());
        assertEquals(40101, ErrorCode.INVALID_CREDENTIALS.getCode());
        assertEquals(40102, ErrorCode.REPLAY_VALIDATION_FAILED.getCode());
        assertEquals(40103, ErrorCode.SESSION_INVALID.getCode());
        assertEquals(40301, ErrorCode.FORBIDDEN.getCode());
        assertEquals(40401, ErrorCode.RESOURCE_NOT_FOUND.getCode());
        assertEquals(40901, ErrorCode.CONFLICT.getCode());
        assertEquals(42301, ErrorCode.ACCOUNT_LOCKED.getCode());
        assertEquals(42901, ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
        assertEquals(50001, ErrorCode.INTERNAL_ERROR.getCode());
    }

    @Test
    @DisplayName("Should extend RuntimeException")
    void testIsRuntimeException() {
        BusinessException ex = new BusinessException(
                ErrorCode.INTERNAL_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong");

        assertInstanceOf(RuntimeException.class, ex);
    }

    @Test
    @DisplayName("Resource not found should use 404 status")
    void testNotFound() {
        BusinessException ex = new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Session not found");

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals(40401, ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Account locked should use 423 status")
    void testAccountLocked() {
        BusinessException ex = new BusinessException(
                ErrorCode.ACCOUNT_LOCKED,
                HttpStatus.LOCKED,
                "Too many failed login attempts");

        assertEquals(HttpStatus.LOCKED, ex.getStatus());
        assertEquals(42301, ex.getErrorCode().getCode());
    }

    @Test
    @DisplayName("Rate limit exceeded should use 429 status")
    void testRateLimitExceeded() {
        BusinessException ex = new BusinessException(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                HttpStatus.TOO_MANY_REQUESTS,
                "Request rate exceeded");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }
}
