package com.prdv.rdv.iam.domain.model.auth;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Delegation temporaire de droits (ex : un medecin donne temporairement a sa
 * secretaire la permission {@code prescription.write} pendant son absence).
 *
 * <p>La delegation est bornee dans le temps et revoquable instantanement ;
 * elle participe au calcul des autorisations cote {@code AuthorizationQuery}.
 */
@Getter
@Setter
public class Delegation {

    private Long id;
    private Long granterUserId;
    private Long granteeUserId;
    private Set<String> permissionCodes = new HashSet<>();
    private String reason;
    private Instant validFrom;
    private Instant validUntil;
    private Instant createdAt;
    private Instant revokedAt;

    public static Delegation grant(Long granterUserId, Long granteeUserId, Set<String> permissionCodes,
                                   String reason, Instant validFrom, Instant validUntil, Clock clock) {
        if (granterUserId.equals(granteeUserId)) {
            throw IamException.of(IamErrorCode.DELEGATION_INVALID,
                    "Impossible de se deleguer des droits a soi-meme");
        }
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            throw IamException.of(IamErrorCode.DELEGATION_INVALID,
                    "La delegation doit comporter au moins une permission");
        }
        if (validUntil != null && validFrom != null && !validUntil.isAfter(validFrom)) {
            throw IamException.of(IamErrorCode.DELEGATION_INVALID,
                    "La date de fin doit etre posterieure a la date de debut");
        }
        Delegation d = new Delegation();
        d.granterUserId = granterUserId;
        d.granteeUserId = granteeUserId;
        d.permissionCodes = new HashSet<>(permissionCodes);
        d.reason = reason;
        d.validFrom = validFrom;
        d.validUntil = validUntil;
        d.createdAt = clock.instant();
        return d;
    }

    public boolean isActive(Clock clock) {
        if (revokedAt != null) {
            return false;
        }
        Instant now = clock.instant();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        return validUntil == null || now.isBefore(validUntil);
    }

    public void revoke(Clock clock) {
        this.revokedAt = clock.instant();
    }
}
