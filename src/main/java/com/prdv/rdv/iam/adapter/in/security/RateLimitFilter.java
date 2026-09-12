package com.prdv.rdv.iam.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.iam.config.IamProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtre de limitation de debit (protection anti-force-brute / mini DDoS) :
 * fenetres fixes d'une minute par IP sur les endpoints sensibles.
 *
 * <p>En production, cette protection est portee par la gateway / WAF et Redis ;
 * ce filtre illustre le point d'extension sans dependance externe.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record WindowKey(String ip, String bucket, long minute) {
    }

    private final ConcurrentHashMap<WindowKey, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final IamProperties properties;
    private final ObjectMapper objectMapper;
    private volatile long lastSweep = System.currentTimeMillis();

    public RateLimitFilter(IamProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String bucket = bucketFor(request.getRequestURI());
        if (bucket != null) {
            sweepIfNeeded();
            int limit = "otp".equals(bucket)
                    ? properties.getSecurity().getRateLimit().getOtpPerMinute()
                    : properties.getSecurity().getRateLimit().getLoginAttemptsPerMinute();
            long minute = Instant.now().getEpochSecond() / 60;
            WindowKey key = new WindowKey(clientIp(request), bucket, minute);
            int count = counters.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            if (count > limit) {
                writeTooManyRequests(response, request);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String bucketFor(String uri) {
        if (uri == null) {
            return null;
        }
        if (uri.startsWith("/api/v1/auth/login")) {
            return "login";
        }
        if (uri.startsWith("/api/v1/auth/otp")) {
            return "otp";
        }
        return null;
    }

    private void sweepIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastSweep < 60_000) {
            return;
        }
        lastSweep = now;
        long currentMinute = Instant.now().getEpochSecond() / 60;
        counters.keySet().removeIf(k -> k.minute() < currentMinute);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "status", 429,
                "code", "RATE_LIMIT_EXCEEDED",
                "message", "Trop de tentatives, reessayez dans une minute",
                "path", request.getRequestURI()));
    }
}
