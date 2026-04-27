package com.research.backend.observability;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.research-requests-per-minute:10}")
    private int researchRequestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:20}")
    private int authRequestsPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientKey = resolveClientKey(request);

        if (path.contains("/v1/research") || path.contains("/v1/auth")) {
            int limit = path.contains("/v1/research")
                    ? researchRequestsPerMinute : authRequestsPerMinute;

            Bucket bucket = buckets.computeIfAbsent(
                    clientKey + ":" + (path.contains("/v1/research") ? "research" : "auth"),
                    k -> newBucket(limit)
            );

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for client={}, path={}", clientKey, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":60}"
                );
                return;
            }

            long remaining = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        }

        chain.doFilter(request, response);
    }

    private Bucket newBucket(int requestsPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestsPerMinute)
                        .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        // Prefer X-Forwarded-For for accurate client IP behind proxies
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
