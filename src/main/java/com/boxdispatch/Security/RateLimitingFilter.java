package com.boxdispatch.Security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client rate limiting using token-bucket algorithm.
 *
 * Resolution: prefer X-Forwarded-For (set by real reverse proxies and the host
 * machine when traffic enters from outside the Docker network). Fall back to
 * RemoteAddr only when no forwarded header exists — which is fine for true
 * inter-container calls (e.g. health checks, internal service calls).
 *
 * Rate limits (matches README):
 *   POST /api/auth/login     →  5 req/min  (burst 10)
 *   POST /api/auth/register  →  3 req/hour
 *   everything else          → 100 req/min
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // key = "<ip>:<tier>"  →  bucket
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp  = resolveClientIp(request);
        String path      = request.getServletPath();
        String method    = request.getMethod();
        String bucketKey = clientIp + ":" + tier(method, path);

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> buildBucket(method, path));

        if (bucket.tryConsume(1)) {
            response.setHeader("X-Rate-Limit-Remaining",
                    String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                "{\"success\":false," +
                "\"message\":\"Too many requests — please slow down and try again shortly.\"," +
                "\"status\":429}"
            );
        }
    }

    /**
     * Priority:
     *   1. X-Forwarded-For first value (set by nginx, AWS ALB, or the Docker host)
     *   2. X-Real-IP (nginx convention)
     *   3. RemoteAddr (fallback — will be the Docker gateway inside a bridge network)
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }

    private String tier(String method, String path) {
        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/api/auth/login"))    return "login";
            if (path.equals("/api/auth/register")) return "register";
        }
        return "default";
    }

    private Bucket buildBucket(String method, String path) {
        String t = tier(method, path);
        return switch (t) {
            // 5 tokens/min, burst up to 10
            case "login"    -> Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.intervally(5,  Duration.ofMinutes(1))))
                    .build();
            // 3 tokens/hour
            case "register" -> Bucket.builder()
                    .addLimit(Bandwidth.classic(3,  Refill.intervally(3,  Duration.ofHours(1))))
                    .build();
            // 100 tokens/min
            default         -> Bucket.builder()
                    .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                    .build();
        };
    }
}