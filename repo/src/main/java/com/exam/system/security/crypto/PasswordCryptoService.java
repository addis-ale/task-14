package com.exam.system.security.crypto;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Password hashing and verification using BCrypt cost factor 12.
 */
@Component
public class PasswordCryptoService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
