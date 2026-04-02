package com.exam.system.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

    private final Object details;

    public ConflictException(String message, Object details) {
        super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}
