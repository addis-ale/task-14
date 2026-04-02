package com.exam.system.config;

import com.exam.system.dto.ApiResponse;
import com.exam.system.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class JsonResponseWriter {

    private final ObjectMapper objectMapper;

    public JsonResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeError(HttpServletResponse response, int httpStatus, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = new ApiResponse<>();
        body.setCode(errorCode.getCode());
        body.setMessage(message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
