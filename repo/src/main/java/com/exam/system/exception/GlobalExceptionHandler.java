package com.exam.system.exception;

import com.exam.system.dto.ApiResponse;
import com.exam.system.dto.ErrorDetail;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(BusinessException ex) {
        ApiResponse<Object> response = new ApiResponse<>();
        response.setCode(ex.getErrorCode().getCode());
        response.setMessage(ex.getMessage());
        if (ex instanceof ConflictException conflictException) {
            response.setData(conflictException.getDetails());
        }
        response.setTimestamp(Instant.now().getEpochSecond());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorDetail)
                .collect(Collectors.toList());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.VALIDATION_FAILED.getCode());
        response.setMessage("Validation failed");
        response.setErrors(details);
        response.setTimestamp(Instant.now().getEpochSecond());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<ErrorDetail> details = ex.getConstraintViolations()
                .stream()
                .map(v -> new ErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .collect(Collectors.toList());

        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.VALIDATION_FAILED.getCode());
        response.setMessage("Validation failed");
        response.setErrors(details);
        response.setTimestamp(Instant.now().getEpochSecond());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(ErrorCode.INTERNAL_ERROR.getCode());
        response.setMessage("Internal server error");
        response.setTimestamp(Instant.now().getEpochSecond());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONNECTION, "close")
                .body(response);
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
