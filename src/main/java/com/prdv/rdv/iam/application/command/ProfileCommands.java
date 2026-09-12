package com.prdv.rdv.iam.application.command;

import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;

import java.time.LocalDate;

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

    /** Rattachement d'un remplaçant sur une plage donnee (= membership REPLACER). */
    public record DeclareReplacement(Long establishmentUserId,
                                     LocalDate validFrom,
                                     LocalDate validUntil) {
    }

    public record ReviewMembership(Long membershipId, boolean approved) {
    }

    public record ImportExternalProfile(String format, String rawContent, String sourcePlatform) {
    }
}
