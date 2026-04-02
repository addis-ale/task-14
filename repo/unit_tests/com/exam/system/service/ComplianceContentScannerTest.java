package com.exam.system.service;

import com.exam.system.service.impl.notification.ComplianceContentScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComplianceContentScanner — scans notification content for
 * sensitive terms, minor-protection terms, and body length.
 */
@DisplayName("ComplianceContentScanner Tests")
class ComplianceContentScannerTest {

    private ComplianceContentScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new ComplianceContentScanner();
    }

    @Test
    @DisplayName("Should return empty list for clean content")
    void testCleanContent() {
        List<String> findings = scanner.scan("期末考试安排通知", "请注意考试时间和地点");
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Should detect sensitive keyword 'violence'")
    void testDetectViolence() {
        List<String> findings = scanner.scan("Warning", "Content contains violence");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("violence"));
    }

    @Test
    @DisplayName("Should detect sensitive keyword 'self-harm'")
    void testDetectSelfHarm() {
        List<String> findings = scanner.scan("Alert", "Mentions self-harm behavior");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("self-harm"));
    }

    @Test
    @DisplayName("Should detect sensitive keyword 'abuse'")
    void testDetectAbuse() {
        List<String> findings = scanner.scan("Report", "Cases of abuse detected");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("abuse"));
    }

    @Test
    @DisplayName("Should detect sensitive keyword 'explicit'")
    void testDetectExplicit() {
        List<String> findings = scanner.scan("Content", "This is explicit material");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("explicit"));
    }

    @Test
    @DisplayName("Should detect minor-protection keyword 'medical'")
    void testDetectMedical() {
        List<String> findings = scanner.scan("Health", "Requires medical attention");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("Minor-protection"));
    }

    @Test
    @DisplayName("Should detect minor-protection keyword 'diagnosis'")
    void testDetectDiagnosis() {
        List<String> findings = scanner.scan("Report", "Includes diagnosis information");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("diagnosis"));
    }

    @Test
    @DisplayName("Should detect minor-protection keyword 'mental health'")
    void testDetectMentalHealth() {
        List<String> findings = scanner.scan("Notice", "Mental health resources available");
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("mental health"));
    }

    @Test
    @DisplayName("Should detect multiple keywords in same content")
    void testMultipleKeywords() {
        List<String> findings = scanner.scan("Alert", "Report about violence and abuse");
        assertEquals(2, findings.size());
    }

    @Test
    @DisplayName("Should flag long body content over 1200 characters")
    void testLongBody() {
        String longBody = "a".repeat(1201);
        List<String> findings = scanner.scan("Test", longBody);
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).contains("Long body content"));
    }

    @Test
    @DisplayName("Should NOT flag body content at exactly 1200 characters")
    void testExactly1200Chars() {
        String body = "a".repeat(1200);
        List<String> findings = scanner.scan("Test", body);
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Should handle null title and body gracefully")
    void testNullInputs() {
        List<String> findings = scanner.scan(null, null);
        assertTrue(findings.isEmpty());
    }

    @Test
    @DisplayName("Should detect keywords case-insensitively")
    void testCaseInsensitive() {
        List<String> findings = scanner.scan("VIOLENCE Warning", "ABUSE detected");
        assertEquals(2, findings.size());
    }

    @Test
    @DisplayName("Should detect keyword in title even when body is clean")
    void testKeywordInTitle() {
        List<String> findings = scanner.scan("Violence alert", "Safe content here");
        assertEquals(1, findings.size());
    }
}
