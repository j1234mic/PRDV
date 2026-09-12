package com.prdv.rdv.iam.domain.model.verification;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Contrat electronique d'adhesion du praticien a la plateforme.
 * On conserve le hash du contenu accepte (non-repudiation) et les
 * circonstances de signature (horodatage, IP).
 */
@Getter
@Setter
public class PractitionerContract {

    private Long id;
    private Long practitionerUserId;
    private String version;
    private String contentHash;
    private Instant acceptedAt;
    private String ipAddress;

    public static PractitionerContract accept(Long practitionerUserId, String version,
                                              String contentHash, String ipAddress, Clock clock) {
        PractitionerContract c = new PractitionerContract();
        c.practitionerUserId = practitionerUserId;
        c.version = version;
        c.contentHash = contentHash;
        c.ipAddress = ipAddress;
        c.acceptedAt = clock.instant();
        return c;
    }
}
