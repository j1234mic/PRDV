package com.prdv.rdv.iam.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ecrit les erreurs de securite de la chaine de filtres en JSON
 * (le GlobalExceptionHandler MVC ne couvre pas les filtres).
 */
@Component
public class SecurityRestHandlers {

    private final ObjectMapper objectMapper;

    public SecurityRestHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response,
                AuthenticationException authException) -> write(response, request, 401,
                "UNAUTHORIZED", "Authentification requise");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                AccessDeniedException accessDeniedException) -> write(response, request, 403,
                "FORBIDDEN", "Permission manquante");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, int status,
                       String code, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "status", status,
                "code", code,
                "message", message,
                "path", request.getRequestURI()));
    }
}
