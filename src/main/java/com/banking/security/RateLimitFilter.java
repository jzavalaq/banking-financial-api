package com.banking.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter for API protection.
 *
 * Enforces rate limits based on:
 * - Auth endpoints (/api/v1/auth/**): 10 requests per minute
 * - General endpoints: 100 requests per minute
 *
 * Uses Bucket4j token bucket algorithm for distributed rate limiting.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.auth.capacity:10}")
    private int authCapacity;

    @Value("${rate-limit.auth.refill-tokens:10}")
    private int authRefillTokens;

    @Value("${rate-limit.auth.refill-duration:60}")
    private int authRefillDuration;

    @Value("${rate-limit.general.capacity:100}")
    private int generalCapacity;

    @Value("${rate-limit.general.refill-tokens:100}")
    private int generalRefillTokens;

    @Value("${rate-limit.general.refill-duration:60}")
    private int generalRefillDuration;

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        boolean isAuthEndpoint = path.startsWith("/api/v1/auth/");
        Map<String, Bucket> bucketMap = isAuthEndpoint ? authBuckets : generalBuckets;

        Bucket bucket = bucketMap.computeIfAbsent(clientIp, k -> isAuthEndpoint
                ? createAuthBucket()
                : createGeneralBucket()
        );

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("X-RateLimit-Limit", isAuthEndpoint ? String.valueOf(authCapacity) : String.valueOf(generalCapacity));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(waitForRefill));
            response.setHeader("Retry-After", String.valueOf(waitForRefill));
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");

            String errorMessage = "{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + waitForRefill + "}";
            response.getWriter().write(errorMessage);

            log.warn("Rate limit exceeded for IP: {}, Path: {}, EndpointType: {}",
                    clientIp, path, isAuthEndpoint ? "AUTH" : "GENERAL");
        }
    }

    private Bucket createAuthBucket() {
        Refill refill = Refill.greedy(authRefillTokens, Duration.ofSeconds(authRefillDuration));
        Bandwidth limit = Bandwidth.classic(authCapacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket createGeneralBucket() {
        Refill refill = Refill.greedy(generalRefillTokens, Duration.ofSeconds(generalRefillDuration));
        Bandwidth limit = Bandwidth.classic(generalCapacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for actuator health checks
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }
}
