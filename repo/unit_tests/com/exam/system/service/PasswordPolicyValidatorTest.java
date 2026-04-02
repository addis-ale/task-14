package com.exam.system.service;

import com.exam.system.exception.BusinessException;
import com.exam.system.service.impl.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordPolicyValidator — validates password strength requirements:
 * minimum 12 characters, requires upper, lower, digit, special character.
 */
@DisplayName("PasswordPolicyValidator Tests")
class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Test
    @DisplayName("Should accept a strong password meeting all requirements")
    void testValidPassword() {
        assertDoesNotThrow(() -> validator.validate("Abcdefgh1234!"));
    }

    @Test
    @DisplayName("Should accept password with exactly 12 characters")
    void testMinLengthPassword() {
        assertDoesNotThrow(() -> validator.validate("Abcdefgh12!@"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Should reject null and empty passwords")
    void testNullAndEmpty(String password) {
        assertThrows(BusinessException.class, () -> validator.validate(password));
    }

    @Test
    @DisplayName("Should reject password shorter than 12 characters")
    void testTooShort() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> validator.validate("Abc1!efgh"));
        assertTrue(ex.getMessage().contains("12 chars"));
    }

    @Test
    @DisplayName("Should reject password without uppercase letter")
    void testNoUppercase() {
        assertThrows(BusinessException.class,
                () -> validator.validate("abcdefgh1234!"));
    }

    @Test
    @DisplayName("Should reject password without lowercase letter")
    void testNoLowercase() {
        assertThrows(BusinessException.class,
                () -> validator.validate("ABCDEFGH1234!"));
    }

    @Test
    @DisplayName("Should reject password without digit")
    void testNoDigit() {
        assertThrows(BusinessException.class,
                () -> validator.validate("Abcdefghijkl!"));
    }

    @Test
    @DisplayName("Should reject password without special character")
    void testNoSpecialChar() {
        assertThrows(BusinessException.class,
                () -> validator.validate("Abcdefgh1234"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Str0ng!Pass#2026",
        "MyP@ssw0rd!2026",
        "C0mpl3x!Pass#",
        "Te$t1ngP@ssw0rd"
    })
    @DisplayName("Should accept various strong passwords")
    void testVariousStrongPasswords(String password) {
        assertDoesNotThrow(() -> validator.validate(password));
    }
}
