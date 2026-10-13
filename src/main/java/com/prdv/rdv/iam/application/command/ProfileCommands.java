package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Commandes liees aux profils, documents KYC, contrats et rattachements.
 */
public final class ProfileCommands {

    private ProfileCommands() {
    }

    /** Depot d'un document justatif (le contenu binaire est transporte par la commande,
     *  jamais de dependance servlet dans la couche application). */
    public record UploadKycDocument(
            Long ownerUserId,
            KycDocument.DocumentType type,
            String originalFilename,
            String contentType,
            byte[] content) {
    }

    public record AcceptContract(String version, String contentHash, String ipAddress) {
    }

    public record RequestMembership(Long establishmentUserId,
                                    EstablishmentMembership.MemberRole role,
                                    LocalDate validFrom,
                                    LocalDate validUntil) {
    }

    /**
     * Designation par un praticien rattache a l'etablissement d'un autre praticien comme
     * remplacant sur une plage donnee (membership REPLACER, en attente de validation).
     */
    public record DesignateReplacer(Long establishmentUserId,
                                    Long replacerPractitionerUserId,
                                    LocalDate validFrom,
                                    LocalDate validUntil) {
    }

    /** Rattachement d'un remplaçant sur une plage donnee (= membership REPLACER). */
    public record DeclareReplacement(Long establishmentUserId,
                                     LocalDate validFrom,
                                     LocalDate validUntil) {
    }

    public record ReviewMembership(Long membershipId, boolean approved) {
    }

    public record ImportExternalProfile(String format, String rawContent, String sourcePlatform) {
    }

    /** Validation de l'assurance responsabilite civile professionnelle (RC Pro) du praticien. */
    public record VerifyInsurance(String company, String policyNumber, LocalDate expiresAt) {
    }

    /** Mise a jour et verification du RIB/IBAN du praticien pour les paiements. */
    public record UpdateBankAccount(String iban) {
    }

    /** Gestion des droits granulaires d'une secretaire medicale. */
    public record UpdateSecretaryPermissions(Long secretaryUserId, Set<String> permissionCodes) {
    }

    /** Gestion des praticiens supervises et de l'acces multi-cabinets d'une secretaire. */
    public record UpdateSecretaryAssignments(Long secretaryUserId,
                                             Set<Long> supervisedPractitionerIds,
                                             Set<Long> accessibleEstablishmentIds) {
    }

    /** Gestion des services / departements d'un etablissement de sante. */
    public record UpdateDepartments(List<String> departments) {
    }

    /**
     * Emission d'une facture groupee pour un etablissement multi-praticiens.
     * {@code platformFeeRatePercent} : commission de la plateforme en pourcentage (0 a 100) ;
     * {@code null} applique le taux par defaut.
     */
    public record IssueGroupInvoice(LocalDate periodStart,
                                    LocalDate periodEnd,
                                    String department,
                                    Set<Long> practitionerUserIds,
                                    int consultationsCount,
                                    long totalAmountCents,
                                    Integer platformFeeRatePercent) {
    }
}
