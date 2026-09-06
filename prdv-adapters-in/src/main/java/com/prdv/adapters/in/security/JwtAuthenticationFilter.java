package com.prdv.adapters.in.security;

import com.prdv.identity.application.port.out.TokenVerifier;
import com.prdv.identity.domain.model.TokenType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ADAPTATEUR primaires de securite : traduit l'en-tete Authorization Bearer en
 * Authentication Spring, via le PORT TokenVerifier (implante par l'adaptateur JWT
 * a l'autre bout de l'hexagone). Le controleur ne manipule jamais de JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenVerifier tokens;

    public JwtAuthenticationFilter(TokenVerifier tokens) {
        this.tokens = tokens;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            tokens.verify(header.substring(PREFIX.length()), TokenType.ACCESS).ifPresent(payload -> {
                AuthenticatedUser principal = new AuthenticatedUser(payload.userId(), payload.email(), payload.role());
                var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + payload.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}
