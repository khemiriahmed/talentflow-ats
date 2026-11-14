package com.talentflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentflow.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitation de débit en mémoire sur les endpoints d'authentification sensibles.
 * Suffisante pour une instance ; en production multi-instances, la même logique peut
 * s'appuyer sur Redis (déjà présent dans le socle).
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.auth.max-requests:100}")
    private int maxRequests;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/v1/auth/login")
                && !path.equals("/api/v1/auth/register")
                && !path.equals("/api/v1/auth/forgot-password")
                && !path.equals("/api/v1/auth/reset-password")
                && !path.equals("/api/v1/auth/verify-email/resend");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr() + "|" + request.getRequestURI();
        Instant now = Instant.now();
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || current.resetAt().isBefore(now)) {
                return new Window(now.plus(WINDOW), 1);
            }
            return new Window(current.resetAt(), current.count() + 1);
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().resetAt().isBefore(now));
        }
        if (window.count() > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(Duration.between(now, window.resetAt()).toSeconds()));
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(),
                    ApiError.of(HttpStatus.TOO_MANY_REQUESTS,
                            "Trop de tentatives, merci de réessayer dans une minute",
                            request.getRequestURI(), null));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private record Window(Instant resetAt, int count) {
    }
}
