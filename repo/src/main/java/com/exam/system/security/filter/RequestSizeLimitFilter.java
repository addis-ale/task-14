package com.exam.system.security.filter;

import com.exam.system.config.JsonResponseWriter;
import com.exam.system.config.SecurityProperties;
import com.exam.system.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private static final long MULTIPART_LIMIT = 50L * 1024L * 1024L;

    private final SecurityProperties securityProperties;
    private final JsonResponseWriter jsonResponseWriter;

    public RequestSizeLimitFilter(SecurityProperties securityProperties, JsonResponseWriter jsonResponseWriter) {
        this.securityProperties = securityProperties;
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > 0) {
            String contentType = request.getContentType();
            boolean multipart = contentType != null && contentType.toLowerCase().startsWith("multipart/");
            long limit = multipart ? MULTIPART_LIMIT : securityProperties.getRequestSizeMaxBytes();
            if (contentLength > limit) {
                jsonResponseWriter.writeError(response, HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        ErrorCode.VALIDATION_FAILED, "Request body exceeds allowed size");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
