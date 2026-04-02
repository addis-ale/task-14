package com.exam.system.security.filter;

import com.exam.system.config.JsonResponseWriter;
import com.exam.system.exception.ErrorCode;
import com.exam.system.security.rbac.UserContext;
import com.exam.system.security.rbac.UserContextHolder;
import com.exam.system.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-based authentication filter and request user-context injection.
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final SessionService sessionService;
    private final JsonResponseWriter jsonResponseWriter;

    public AuthFilter(SessionService sessionService, JsonResponseWriter jsonResponseWriter) {
        this.sessionService = sessionService;
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return ("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/login".equals(uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.SESSION_INVALID, "Missing or invalid Authorization header");
            return;
        }

        String token = authorization.substring(7);
        UserContext context = sessionService.resolve(token).orElse(null);
        if (context == null) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.SESSION_INVALID, "Session is invalid or expired");
            return;
        }

        try {
            UserContextHolder.set(context);
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }
}
