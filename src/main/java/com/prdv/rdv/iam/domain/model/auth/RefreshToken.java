package com.prdv.rdv.iam.domain.model.auth;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Jeton de rafraichissement. Stocke sous forme hachee (SHA-256).
 *
 * <p>Rotation : chaque utilisation cree un nouveau jeton de la meme {@code family}
 * et revoque l'ancien. La presentation d'un jeton deja revoque signale une
 * reutilisation frauduleuse : toute la famille est revoquee.
 */
@Getter
@Setter
public class RefreshToken {

    private Long id;
    private Long userId;
    private String tokenHash;
    private String family;
    private Long sessionId;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;

    public static RefreshToken create(Long userId, String tokenHash, String family,
                                      Long sessionId, long ttlDays, Clock clock) {
        RefreshToken t = new RefreshToken();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.family = family;
        t.sessionId = sessionId;
        t.createdAt = clock.instant();
        t.expiresAt = t.createdAt.plus(ttlDays, ChronoUnit.DAYS);
        return t;
    }

    public boolean isValid(Clock clock) {
        return !revoked && clock.instant().isBefore(expiresAt);
    }

    public void revoke(Clock clock) {
        this.revoked = true;
        this.revokedAt = clock.instant();
    }
}
