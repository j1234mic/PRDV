package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.time.Instant;

/**
 * Value Object : badge de visibilite d'un praticien
 * (verifie / populaire / nouveau).
 *
 * <p>Un badge est toujours justifie ({@code reason}) et porte une provenance :
 * automatique (regle {@link BadgePolicy}) ou manuel (equipe de moderation).
 * Les badges automatiques peuvent expirer, ce qui evite qu'un badge
 * « populaire » reste acquis a vie.
 */
public record Badge(BadgeType type,
                    BadgeSource source,
                    String reason,
                    Instant grantedAt,
                    Instant expiresAt) {

    public enum BadgeType {
        /** Identite, diplomes, assurance et RIB verifies. */
        VERIFIED,
        /** Volume d'avis et note moyenne elevee. */
        POPULAR,
        /** Inscription recente (mise en avant des nouveaux praticiens). */
        NEW
    }

    public enum BadgeSource { AUTOMATIC, MANUAL }

    public Badge {
        if (type == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le type de badge est obligatoire");
        }
        if (grantedAt == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date d'attribution du badge est obligatoire");
        }
        if (expiresAt != null && expiresAt.isBefore(grantedAt)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un badge ne peut pas expirer avant son attribution");
        }
        source = source == null ? BadgeSource.AUTOMATIC : source;
        reason = reason == null ? type.name() : reason;
    }

    public boolean isActive(Instant now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
