package com.exam.system.dto;

import java.time.Instant;
import java.util.List;

public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private List<ErrorDetail> errors;

    public ApiResponse() {
        this.timestamp = Instant.now().getEpochSecond();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static ApiResponse<Void> successMessage(String message) {
        ApiResponse<Void> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        return response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<ErrorDetail> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorDetail> errors) {
        this.errors = errors;
    }
}
