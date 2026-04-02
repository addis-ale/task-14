package com.exam.system.service.impl;

import com.exam.system.exception.BusinessException;
import com.exam.system.exception.ErrorCode;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

    public void validate(String password) {
        if (password == null || password.length() < 12
                || !UPPER.matcher(password).matches()
                || !LOWER.matcher(password).matches()
                || !DIGIT.matcher(password).matches()
                || !SPECIAL.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 chars and include upper, lower, digit and special character");
        }
    }
}
