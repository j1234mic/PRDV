package com.prdv.rdv.iam.domain.model.user;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

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

    /** Assurance responsabilite civile professionnelle (RC Pro). */
    private String insuranceCompany;
    private String insurancePolicyNumber;
    private LocalDate insuranceExpiresAt;

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

    /** Enregistre et valide l'assurance responsabilite civile professionnelle (RC Pro). */
    public void recordVerifiedInsurance(String company, String policyNumber,
                                        LocalDate expiresAt, Clock clock) {
        if (company == null || company.isBlank() || policyNumber == null || policyNumber.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La compagnie d'assurance et le numero de police RC Pro sont obligatoires");
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDate.now(clock))) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "L'attestation d'assurance RC Pro est expiree (" + expiresAt + ")");
        }
        this.insuranceCompany = company.trim();
        this.insurancePolicyNumber = policyNumber.trim();
        this.insuranceExpiresAt = expiresAt;
        this.professionalInsuranceVerified = true;
        this.updatedAt = clock.instant();
    }
}
