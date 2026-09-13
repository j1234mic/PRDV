package com.prdv.rdv.iam.adapter.in.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de la chaine de securite.
 *
 * <p>Politique : REST statique (JWT) pour l'API, endpoints publics minimaux,
 * autorisations RBAC verifiees par annotation sur les controleurs.
 * Le login OAuth2/OIDC n'est active que si un fournisseur est configure
 * (profil "social").
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityRestHandlers restHandlers;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final ObjectProvider<AuthenticationSuccessHandler> socialSuccessHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          SecurityRestHandlers restHandlers,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
                          ObjectProvider<AuthenticationSuccessHandler> socialSuccessHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.restHandlers = restHandlers;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.socialSuccessHandler = socialSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register/**",
                                "/api/v1/auth/login", "/api/v1/auth/login/**",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/otp/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/practitioners/register",
                                "/api/v1/establishments/register").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error", "/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(restHandlers.accessDeniedHandler()))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // OAuth2 / OIDC (Google, Apple, SSO entreprise) - actif avec le profil "social"
        if (clientRegistrationRepository.getIfAvailable() != null) {
            AuthenticationSuccessHandler handler = socialSuccessHandler.getIfAvailable();
            http.oauth2Login(oauth -> {
                if (handler != null) {
                    oauth.successHandler(handler);
                }
            });
        }
        return http.build();
    }

    /**
     * Les filtres de securite sont des beans (injectes dans la chaine) : on
     * desactive leur enregistrement automatique par le conteneur pour eviter
     * une double execution.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:[*]", "https://*.prdv.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
