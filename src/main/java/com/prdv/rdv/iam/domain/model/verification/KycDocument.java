package com.prdv.rdv.iam.domain.model.verification;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;

/**
 * Document justatif telecharge (CNI, passeport, diplome, RIB, attestation
 * d'assurance responsabilite civile, contrat signe). Le fichier reside chez
 * un {@code DocumentStoragePort} (disque local en dev, S3 en production) ;
 * seule la cle de stockage est conservee en base.
 */
@Getter
@Setter
public class KycDocument {

    public enum DocumentType {
        IDENTITY_CARD,
        PASSPORT,
        DIPLOMA,
        RIB,
        PROFESSIONAL_INSURANCE,
        SIGNED_CONTRACT,
        COMPANY_REGISTRATION
    }

    public enum ReviewStatus { PENDING, VERIFIED, REJECTED }

    private Long id;
    private Long ownerUserId;
    private DocumentType type;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private long sizeBytes;
    private ReviewStatus status = ReviewStatus.PENDING;
    private String reviewNote;
    private Instant uploadedAt;
    private Instant reviewedAt;

    public static KycDocument upload(Long ownerUserId, DocumentType type, String storageKey,
                                     String originalFilename, String contentType, long sizeBytes,
                                     Clock clock) {
        KycDocument d = new KycDocument();
        d.ownerUserId = ownerUserId;
        d.type = type;
        d.storageKey = storageKey;
        d.originalFilename = originalFilename;
        d.contentType = contentType;
        d.sizeBytes = sizeBytes;
        d.uploadedAt = clock.instant();
        return d;
    }

    public void review(ReviewStatus newStatus, String note, Clock clock) {
        if (this.status != ReviewStatus.PENDING) {
            throw IamException.of(IamErrorCode.APPLICATION_ALREADY_REVIEWED,
                    "Document deja examine");
        }
        if (newStatus == ReviewStatus.PENDING) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La decision de revue doit etre VERIFIED ou REJECTED");
        }
        this.status = newStatus;
        this.reviewNote = note;
        this.reviewedAt = clock.instant();
    }
}
