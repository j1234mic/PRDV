package com.prdv.rdv.iam.application.port.output;

import java.time.Instant;
import java.util.Collection;

/**
 * Port des jetons JWT (signature / verification).
 * Implementation avec Nimbus JOSE+JWT (HMAC-SHA256, tokens a courte duree
 * + refresh tokens opaques tournants).
 */
public interface JwtTokenPort {

    String createAccessToken(Long userId, String email, Collection<String> authorities, long tokenVersion);

    /** Jeton borne a l'authentification MFA/OTP (aucune permission, juste l'identite en attente). */
    String createChallengeToken(Long userId, String purpose);

    AccessTokenClaims parseAccessToken(String token);

    ChallengeClaims parseChallengeToken(String token, String expectedPurpose);

    long getAccessTokenTtlSeconds();

    record AccessTokenClaims(Long userId, String email, long tokenVersion, Instant expiresAt) {
    }

    record ChallengeClaims(Long userId, String purpose, Instant expiresAt) {
    }
}
