package com.prdv.rdv.iam.adapter.in.security;

import com.prdv.rdv.iam.config.IamProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Protection anti-DDoS / anti-bruteforce par limitation de debit (algorithme
 * Token Bucket en memoire, indexe par adresse IP, avec bannissement temporaire
 * automatique en cas de rafales repetees).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int OVERFLOW_STRIKES_BEFORE_BAN = 5;
    private static final Duration DDOS_BAN_DURATION = Duration.ofMinutes(10);

    private final IamProperties properties;
    private final WafDdosProtectionRegistry registry;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> overflowStrikes = new ConcurrentHashMap<>();

    public RateLimitFilter(IamProperties properties) {
        this(properties, null);
    }

    @Autowired
    public RateLimitFilter(IamProperties properties, WafDdosProtectionRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean authEndpoint = path != null && (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/register"));
        int limitPerMinute = authEndpoint
                ? properties.getSecurity().getRateLimit().getAuthRequestsPerMinute()
                : properties.getSecurity().getRateLimit().getRequestsPerMinute();

        String ip = clientIp(request);
        if (registry != null && registry.isIpBlocked(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"Adresse IP temporairement bloquee suite a une rafale DDoS\"}");
            return;
        }

        String key = ip + ":" + (authEndpoint ? "auth" : "general");
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(limitPerMinute));

        if (!bucket.tryConsume(limitPerMinute)) {
            int strikes = overflowStrikes.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
            if (strikes >= OVERFLOW_STRIKES_BEFORE_BAN && registry != null) {
                registry.blockIpTemporarily(ip, DDOS_BAN_DURATION);
            }
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"Trop de requetes, reessayez plus tard\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        Bucket(int initial) {
            this.tokens = initial;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume(int capacityPerMinute) {
            long now = System.nanoTime();
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            double ratePerSecond = capacityPerMinute / 60.0;
            tokens = Math.min(capacityPerMinute, tokens + elapsedSeconds * ratePerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
