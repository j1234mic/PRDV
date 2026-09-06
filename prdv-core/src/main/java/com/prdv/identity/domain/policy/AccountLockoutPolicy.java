package com.prdv.identity.domain.policy;

import com.prdv.identity.domain.model.User;

import java.time.Duration;

/**
 * Pattern STRATEGY : politique de blocage du compte apres tentatives echouees
 * (module 1.2 "Blocage automatique apres tentatives").
 * Le coeur applique la politique sans connaitre sa configuration : une autre
 * politique (ex. blocage progressif, CAPTCHA) s'ajoute sans toucher User/LoginHandler.
 */
public final class AccountLockoutPolicy {

    private final int maxFailedAttempts;
    private final Duration lockDuration;

    public AccountLockoutPolicy(int maxFailedAttempts, Duration lockDuration) {
        if (maxFailedAttempts < 1) {
            throw new IllegalArgumentException("maxFailedAttempts doit etre >= 1");
        }
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = lockDuration;
    }

    /** Enregistre un echec ; bloque le compte si le seuil est atteint. */
    public void onFailedAttempt(User user, java.time.LocalDateTime now) {
        user.registerFailedAttempt(now, maxFailedAttempts, lockDuration);
    }

    /** Reinitialise le compteur apres succes. */
    public void onSuccess(User user) {
        user.resetFailedAttempts();
    }
}
