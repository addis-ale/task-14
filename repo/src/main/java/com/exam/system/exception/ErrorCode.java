package com.exam.system.exception;

public enum ErrorCode {
    VALIDATION_FAILED(40001),
    INVALID_CREDENTIALS(40101),
    REPLAY_VALIDATION_FAILED(40102),
    SESSION_INVALID(40103),
    FORBIDDEN(40301),
    RESOURCE_NOT_FOUND(40401),
    CONFLICT(40901),
    ACCOUNT_LOCKED(42301),
    RATE_LIMIT_EXCEEDED(42901),
    INTERNAL_ERROR(50001);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
