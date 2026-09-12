package com.prdv.rdv.iam.domain.model.verification;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Lien entre un praticien et un etablissement.
 *
 * <p>Un meme praticien peut avoir plusieurs rattachements (multi-cabinets) ;
 * le role {@link MemberRole#REPLACER} modelise la gestion des remplaçants,
 * avec une plage de validite. Le rattachement est accepte par l'etablissement.
 */
@Getter
@Setter
public class EstablishmentMembership {

    public enum MemberRole {
        OWNER,
        EMPLOYEE,
        REPLACER
    }

    public enum MembershipStatus {
        PENDING,
        ACTIVE,
        REJECTED,
        REVOKED
    }

    private Long id;
    private Long establishmentUserId;
    private Long practitionerUserId;
    private MemberRole memberRole;
    private MembershipStatus status;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Instant requestedAt;
    private Instant decidedAt;

    public static EstablishmentMembership request(Long establishmentUserId, Long practitionerUserId,
                                                  MemberRole role, LocalDate validFrom, LocalDate validUntil,
                                                  Clock clock) {
        EstablishmentMembership m = new EstablishmentMembership();
        m.establishmentUserId = establishmentUserId;
        m.practitionerUserId = practitionerUserId;
        m.memberRole = role;
        m.status = MembershipStatus.PENDING;
        m.validFrom = validFrom;
        m.validUntil = validUntil;
        m.requestedAt = clock.instant();
        return m;
    }

    public void accept(Clock clock) {
        this.status = MembershipStatus.ACTIVE;
        this.decidedAt = clock.instant();
    }

    public void reject(Clock clock) {
        this.status = MembershipStatus.REJECTED;
        this.decidedAt = clock.instant();
    }

    public void revoke(Clock clock) {
        this.status = MembershipStatus.REVOKED;
        this.decidedAt = clock.instant();
    }
}
