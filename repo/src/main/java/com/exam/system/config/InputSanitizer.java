package com.exam.system.config;

import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    private InputSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return HTML_TAG.matcher(value).replaceAll("").trim();
    }
}
