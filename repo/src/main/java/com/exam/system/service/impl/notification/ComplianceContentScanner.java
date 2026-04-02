package com.exam.system.service.impl.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ComplianceContentScanner {

    private static final List<String> SENSITIVE_TERMS = List.of(
            "violence",
            "self-harm",
            "abuse",
            "explicit"
    );

    private static final List<String> MINOR_PROTECTION_TERMS = List.of(
            "medical",
            "diagnosis",
            "mental health"
    );

    public List<String> scan(String title, String body) {
        String text = ((title == null ? "" : title) + " " + (body == null ? "" : body)).toLowerCase(Locale.ROOT);
        List<String> findings = new ArrayList<>();

        for (String term : SENSITIVE_TERMS) {
            if (text.contains(term)) {
                findings.add("Sensitive content keyword detected: " + term);
            }
        }
        for (String term : MINOR_PROTECTION_TERMS) {
            if (text.contains(term)) {
                findings.add("Minor-protection review needed for keyword: " + term);
            }
        }
        if (body != null && body.length() > 1200) {
            findings.add("Long body content requires enhanced readability review");
        }
        return findings;
    }
}
