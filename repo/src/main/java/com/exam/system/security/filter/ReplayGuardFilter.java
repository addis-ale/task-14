package com.exam.system.security.filter;

import com.exam.system.config.JsonResponseWriter;
import com.exam.system.config.SecurityProperties;
import com.exam.system.exception.ErrorCode;
import com.exam.system.security.crypto.HmacCryptoService;
import com.exam.system.service.RequestNonceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Prevents replay attacks with timestamp, nonce and request signature checks.
 */
@Component
public class ReplayGuardFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final RequestNonceService requestNonceService;
    private final HmacCryptoService hmacCryptoService;
    private final JsonResponseWriter jsonResponseWriter;

    public ReplayGuardFilter(SecurityProperties securityProperties,
                             RequestNonceService requestNonceService,
                             HmacCryptoService hmacCryptoService,
                             JsonResponseWriter jsonResponseWriter) {
        this.securityProperties = securityProperties;
        this.requestNonceService = requestNonceService;
        this.hmacCryptoService = hmacCryptoService;
        this.jsonResponseWriter = jsonResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if ("/api/v1/health".equals(request.getRequestURI())) {
            return true;
        }
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String timestampHeader = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");

        if (timestampHeader == null || nonce == null || signature == null) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.REPLAY_VALIDATION_FAILED, "Missing replay-guard headers");
            return;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.REPLAY_VALIDATION_FAILED, "Invalid X-Timestamp value");
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > securityProperties.getReplayWindowSeconds()) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.REPLAY_VALIDATION_FAILED, "Request timestamp is outside the allowed window");
            return;
        }

        if (requestNonceService.isDuplicate(nonce)) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.REPLAY_VALIDATION_FAILED, "Duplicate nonce detected");
            return;
        }

        CachedBodyRequestWrapper wrappedRequest = new CachedBodyRequestWrapper(request);
        String bodyHash = HashUtils.sha256Hex(wrappedRequest.getCachedBody());
        String payload = wrappedRequest.getMethod() + wrappedRequest.getRequestURI() + timestampHeader + nonce + bodyHash;

        if (!hmacCryptoService.verify(payload, signature)) {
            jsonResponseWriter.writeError(response, HttpStatus.UNAUTHORIZED.value(),
                    ErrorCode.REPLAY_VALIDATION_FAILED, "Invalid request signature");
            return;
        }

        requestNonceService.save(nonce, securityProperties.getReplayWindowSeconds());
        filterChain.doFilter(wrappedRequest, response);
    }
}
