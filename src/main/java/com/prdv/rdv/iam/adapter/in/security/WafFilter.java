package com.prdv.rdv.iam.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Web Application Firewall (WAF) applicatif : inspecte l'URI, la query string
 * et les en-tetes pour bloquer en amont les vecteurs d'attaque classiques
 * (SQL injection, XSS, traversee de chemin, injection JNDI/Log4Shell,
 * command injection, SSRF metadata cloud).
 */
@Component
public class WafFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(WafFilter.class);

    private static final Map<String, Pattern> RULES;

    static {
        Map<String, Pattern> rules = new LinkedHashMap<>();
        rules.put("SQL_INJECTION", Pattern.compile(
                "(?i)(\\bunion\\b\\s+\\bselect\\b|\\bor\\b\\s+1\\s*=\\s*1|;--|\\bdrop\\s+table\\b|\\bsleep\\s*\\(|\\bbenchmark\\s*\\()"));
        rules.put("XSS_ATTACK", Pattern.compile(
                "(?i)(<\\s*script\\b|javascript\\s*:|onerror\\s*=|onload\\s*=|<\\s*iframe\\b|<\\s*svg\\b[^>]*onload)"));
        rules.put("PATH_TRAVERSAL", Pattern.compile(
                "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e\\\\)", Pattern.CASE_INSENSITIVE));
        rules.put("JNDI_LOG4SHELL", Pattern.compile(
                "(?i)\\$\\{\\s*jndi\\s*:"));
        rules.put("COMMAND_INJECTION", Pattern.compile(
                "(?i)(;\\s*(cat|ls|whoami|curl|wget|bash|sh)\\b|\\|\\|\\s*(id|uname)\\b|`[^`]+`)"));
        rules.put("SSRF_CLOUD_METADATA", Pattern.compile(
                "(?i)(169\\.254\\.169\\.254|metadata\\.google\\.internal)"));
        RULES = Collections.unmodifiableMap(rules);
    }

    private final WafDdosProtectionRegistry registry;

    public WafFilter() {
        this(null);
    }

    @Autowired
    public WafFilter(WafDdosProtectionRegistry registry) {
        this.registry = registry;
        if (this.registry != null) {
            this.registry.setActiveRulesCount(RULES.size());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        if (registry != null && registry.isIpBlocked(ip)) {
            block(response, HttpServletResponse.SC_FORBIDDEN, "IP_BLOCKED_DDOS",
                    "Adresse IP temporairement bannie par la protection WAF/DDoS");
            return;
        }

        String uriRule = matchingRule(request.getRequestURI());
        if (uriRule != null) {
            onMaliciousRequest(request, response, ip, "URI:" + uriRule);
            return;
        }
        String queryRule = matchingRule(request.getQueryString());
        if (queryRule != null) {
            onMaliciousRequest(request, response, ip, "QUERY:" + queryRule);
            return;
        }
        for (String header : Collections.list(request.getHeaderNames())) {
            if ("cookie".equalsIgnoreCase(header) || "authorization".equalsIgnoreCase(header)) {
                continue;
            }
            String headerRule = matchingRule(request.getHeader(header));
            if (headerRule != null) {
                onMaliciousRequest(request, response, ip, "HEADER:" + header + ":" + headerRule);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void onMaliciousRequest(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String ip,
                                    String matchedRule) throws IOException {
        log.warn("[WAF] Requete bloquee ({}) depuis {} sur {} {}",
                matchedRule, ip, request.getMethod(), request.getRequestURI());
        if (registry != null) {
            registry.recordWafBlock(ip, request.getMethod(), request.getRequestURI(), matchedRule);
        }
        block(response, HttpServletResponse.SC_BAD_REQUEST, "WAF_BLOCKED",
                "Requete rejetee par le pare-feu applicatif (" + matchedRule + ")");
    }

    private String matchingRule(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            decoded = raw;
        }
        for (Map.Entry<String, Pattern> entry : RULES.entrySet()) {
            if (entry.getValue().matcher(raw).find() || entry.getValue().matcher(decoded).find()) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    private void block(HttpServletResponse response,
                       int status,
                       String code,
                       String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
