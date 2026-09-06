package com.prdv.identity.domain.model;

/** Cycle de vie d'un compte (machine a etats - cf. User). */
public enum UserStatus {
    /** Compte cree, OTP email non verifie. */
    PENDING_VERIFICATION,
    /** Compte actif, peut se connecter. */
    ACTIVE,
    /** Blocage temporaire apres tentatives echouees repetees. */
    LOCKED,
    /** Suspendu par un administrateur (moderation, fraude...). */
    SUSPENDED
}
