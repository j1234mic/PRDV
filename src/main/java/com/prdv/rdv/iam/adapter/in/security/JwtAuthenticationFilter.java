package com.prdv.rdv.iam.adapter.in.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.iam.application.port.input.AuthorizationQueryUseCase;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Filtre d'authentification Bearer JWT.
 * Le JWT porte l'identite et une version de tokens ; les autorites sont
 * resolues a chaque requete via le cas d'usage (roles, permissions directes
 * et delegations actives) : revelation / revocation immediates.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenPort jwtTokenPort;
    private final UserRepository userRepository;
    private final AuthorizationQueryUseCase authorizationQuery;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenPort jwtTokenPort, UserRepository userRepository,
                                   AuthorizationQueryUseCase authorizationQuery, ObjectMapper objectMapper) {
        this.jwtTokenPort = jwtTokenPort;
        this.userRepository = userRepository;
        this.authorizationQuery = authorizationQuery;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                authenticate(token);
            } catch (IamException ex) {
                SecurityContextHolder.clearContext();
                writeUnauthorized(response, request, ex.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        JwtTokenPort.AccessTokenClaims claims = jwtTokenPort.parseAccessToken(token);
        User user = userRepository.findById(claims.userId()).orElseThrow(
                () -> IamException.of(com.prdv.rdv.iam.domain.exception.IamErrorCode.TOKEN_INVALID,
                        "Compte introuvable"));

        if (user.getTokenVersion() != claims.tokenVersion()) {
            throw IamException.of(com.prdv.rdv.iam.domain.exception.IamErrorCode.TOKEN_INVALID,
                    "Jeton revoque");
        }
        if (user.getStatus() != AccountStatus.ACTIVE
                && user.getStatus() != AccountStatus.PENDING_VALIDATION) {
            throw IamException.of(com.prdv.rdv.iam.domain.exception.IamErrorCode.FORBIDDEN,
                    "Compte non autorise (" + user.getStatus() + ")");
        }

        Set<SimpleGrantedAuthority> authorities = authorizationQuery.authoritiesFor(user.getId()).stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toSet());

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                java.util.Map.of(
                        "timestamp", java.time.Instant.now().toString(),
                        "status", 401,
                        "code", "TOKEN_INVALID",
                        "message", message,
                        "path", request.getRequestURI()));
    }
}
