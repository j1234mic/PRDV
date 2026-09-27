package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.practitioner.BadgePolicy;

/**
 * Port de sortie : etat des verifications reglementaires d'un praticien
 * (RPPS/ADELI, diplomes, assurance RC pro, RIB), detenu par le contexte IAM.
 *
 * <p>Utilise par la {@link BadgePolicy} pour attribuer le badge « verifie »
 * sans que le contexte profils ne lise les tables de l'autre contexte.
 */
public interface PractitionerVerificationPort {

    BadgePolicy.VerificationState verificationOf(Long practitionerUserId);

    /** Date de creation du compte : sert a l'anciennete (badge « nouveau »). */
    java.util.Optional<java.time.Instant> registeredAt(Long practitionerUserId);
}
