package com.prdv.adapters.in.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * RBAC aux FRONTIERES (module 1.2) : qui peut joindre quelle surface.
 * La regle d'autorisation FONCTIONNELLE (proprietaire du RDV, medecin verifie...)
 * est, elle, portee par le domaine -> defense en profondeur.
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> { })
            .headers(headers -> headers.frameOptions(frame -> frame.disable())) // H2 console (dev)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health", "/error", "/h2-console/**").permitAll()
                // annuaire PUBLIC (recherche + fiche praticien), le reste de /api/doctors est protege
                .requestMatchers(HttpMethod.GET, "/api/doctors", "/api/doctors/*/profile", "/api/slots/**")
                    .permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/schedule/**").hasAnyRole("DOCTOR", "ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
