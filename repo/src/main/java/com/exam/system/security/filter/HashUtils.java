package com.exam.system.security.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class HashUtils {

    private HashUtils() {
    }

    public static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash payload", ex);
        }
    }

    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }
}
