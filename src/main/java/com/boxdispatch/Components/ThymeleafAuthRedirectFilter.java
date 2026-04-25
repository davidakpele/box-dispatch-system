package com.boxdispatch.Components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.boxdispatch.Interface.IJwtService;

import java.io.IOException;
import java.util.Set;

/**
 * Intercepts Thymeleaf page routes (non-API, non-static requests).
 *
 * If the request targets a protected page and no valid JWT is present in the
 * Authorization header or the bd_token cookie, the user is redirected to /login
 * instead of receiving a 401 JSON response.
 *
 * API routes (/api/**) are intentionally excluded — they should still return
 * the standard 401 JSON so frontend fetch() calls can handle them properly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ThymeleafAuthRedirectFilter extends OncePerRequestFilter {

    private final IJwtService jwtService;  // your existing JWT service interface


    /** Pages that require a valid JWT — redirect to /login if missing. */
    private static final Set<String> PROTECTED_PAGES = Set.of(
            "/dashboard",
            "/"
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only act on protected page routes — let everything else pass through
        if (!PROTECTED_PAGES.contains(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token == null || !isValidToken(token)) {
            log.debug("No valid JWT for page '{}' — redirecting to /login", path);
            response.sendRedirect("/login");
            return;
        }

        // Token is valid — let the request continue to HomeController
        filterChain.doFilter(request, response);
    }

    /**
     * Tries Authorization header first, then falls back to the bd_token cookie
     * (set by the login page's localStorage → this filter reads the cookie variant).
     *
     * Note: localStorage is NOT accessible server-side. If you want true server-side
     * JWT validation, switch the login page to store the token in an HttpOnly cookie
     * instead of (or in addition to) localStorage. See HomeController note below.
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Authorization: Bearer <token>
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2. HttpOnly cookie named "bd_token"
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("bd_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private boolean isValidToken(String token) {
        try {
            // validateToken checks structure, signature, expiry and claims.
            // validateTokenClaims checks issuer/audience/notBefore.
            // isTokenExpired is redundant with validateToken but adds an explicit
            // expiry check as a safety net — matches your JwtService implementation.
            return jwtService.validateToken(token) && !jwtService.isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /** Skip this filter entirely for API calls and static resources. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.endsWith(".ico")
                || path.endsWith(".css")
                || path.endsWith(".js");
    }
}