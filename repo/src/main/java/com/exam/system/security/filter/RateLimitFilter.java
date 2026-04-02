package com.exam.system.security.filter;

import com.exam.system.config.JsonResponseWriter;
import com.exam.system.config.SecurityProperties;
import com.exam.system.exception.ErrorCode;
import com.exam.system.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sliding-window rate limiter: 60 req/min per user, 300 req/min per IP.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_SECONDS = 60;

    private final SecurityProperties securityProperties;
    private final JsonResponseWriter jsonResponseWriter;
    private final SessionService sessionService;
    private final ConcurrentHashMap<String, Deque<Long>> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(SecurityProperties securityProperties,
                           JsonResponseWriter jsonResponseWriter,
                           SessionService sessionService) {
        this.securityProperties = securityProperties;
        this.jsonResponseWriter = jsonResponseWriter;
        this.sessionService = sessionService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();

        String ip = request.getRemoteAddr();
        String token = extractToken(request.getHeader("Authorization"));
        Optional<Long> userId = token == null ? Optional.empty() : sessionService.resolveUserId(token);
        String userKey = userId.map(id -> "user:" + id).orElse("user:anonymous:" + ip);
        String ipKey = "ip:" + ip;

        int userRemaining = checkAndIncrement(userKey, securityProperties.getUserRateLimitPerMinute(), now);
        if (userRemaining < 0) {
            reject(response);
            return;
        }

        int ipRemaining = checkAndIncrement(ipKey, securityProperties.getIpRateLimitPerMinute(), now);
        if (ipRemaining < 0) {
            reject(response);
            return;
        }

        response.setHeader("X-RateLimit-User-Remaining", String.valueOf(userRemaining));
        response.setHeader("X-RateLimit-IP-Remaining", String.valueOf(ipRemaining));
        filterChain.doFilter(request, response);
    }

    private int checkAndIncrement(String key, int limit, long now) {
        Deque<Long> queue = counters.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() >= WINDOW_SECONDS) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                return -1;
            }
            queue.addLast(now);
            return limit - queue.size();
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setHeader("Retry-After", "60");
        jsonResponseWriter.writeError(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                ErrorCode.RATE_LIMIT_EXCEEDED, "Rate limit exceeded");
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}
