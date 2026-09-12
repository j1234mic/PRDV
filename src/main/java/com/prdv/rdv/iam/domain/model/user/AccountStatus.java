package com.prdv.rdv.iam.domain.model.user;

/**
 * Cycle de vie du compte.
 *
 * <pre>
 * PENDING_EMAIL_VERIFICATION --(OTP)--> PENDING_VALIDATION (praticien/etablissement/secrétaire)
 *                                      ACTIVE (patient)
 * PENDING_VALIDATION --(validation manuelle admin)--> ACTIVE
 * ACTIVE -> LOCKED (trop d'echecs, temporaire) -> ACTIVE
 * ACTIVE -> SUSPENDED (moderation) -> ACTIVE
 * ACTIVE -> DELETED (droit a l'oubli / anonymisation)
 * PENDING_* -> REJECTED (dossier de prerequis refuse)
 * </pre>
 */
public enum AccountStatus {
    PENDING_EMAIL_VERIFICATION,
    PENDING_VALIDATION,
    ACTIVE,
    LOCKED,
    SUSPENDED,
    REJECTED,
    DELETED
}
