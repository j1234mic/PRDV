package com.prdv.rdv.iam.domain.model.user;

import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Profil medecin / praticien.
 *
 * <p>Regroupe les verifications reglementaires :
 * RPPS (Repertoire Partage des Professionnels de Sante), ADELI, diplomes
 * (API Ordre des medecins), RIB (paiements), assurance responsabilite civile
 * professionnelle, et le contrat d'adhesion electronique.
 *
 * <p>Le rattachement multi-etablissements et les remplacements sont portes par
 * {@code EstablishmentMembership} (un praticien peut exercer dans plusieurs cabinets).
 */
@Getter
@Setter
public class PractitionerProfile {

    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String specialty;

    private String rppsNumber;
    private String adeliNumber;

    private boolean rppsVerified;
    private boolean adeliVerified;
    private boolean diplomaVerified;
    private boolean professionalInsuranceVerified;
    private boolean bankAccountVerified;

    /** Iban tokenise (jamais en clair) ; la verification RIB est decouplee. */
    private String ribToken;
    private String maskedIban;

    private String validationNote;

    private Instant createdAt;
    private Instant updatedAt;

    public static PractitionerProfile create(Long userId, String firstName, String lastName,
                                             String specialty, String rppsNumber, String adeliNumber,
                                             Clock clock) {
        PractitionerProfile p = new PractitionerProfile();
        p.userId = userId;
        p.firstName = firstName;
        p.lastName = lastName;
        p.specialty = specialty;
        p.rppsNumber = rppsNumber;
        p.adeliNumber = adeliNumber;
        p.createdAt = clock.instant();
        p.updatedAt = p.createdAt;
        return p;
    }

    /** Le dossier prerequis est-il complet avant validation manuelle par un moderateur ? */
    public boolean isApplicationComplete() {
        return rppsNumber != null && !rppsNumber.isBlank()
                && diplomaVerified
                && professionalInsuranceVerified
                && bankAccountVerified;
    }

    public void storeBankDetails(String ribToken, String maskedIban) {
        this.ribToken = ribToken;
        this.maskedIban = maskedIban;
    }
}
