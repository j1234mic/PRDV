package com.prdv.identity.application.port.out;

import com.prdv.identity.domain.model.TokenType;

import java.util.Optional;

/** Port de sortie : verification des jetons (utilise par le filtre de securite REST). */
public interface TokenVerifier {

    /** Payload d'un jeton valide. */
    record Payload(Long userId, String email, String role, TokenType type) { }

    /** Vide si signature invalide, expiree ou type inattendu. */
    Optional<Payload> verify(String token, TokenType expectedType);
}
