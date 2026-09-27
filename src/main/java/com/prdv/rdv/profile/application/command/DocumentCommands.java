package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;

import java.time.Instant;

/**
 * Commandes du cas d'usage « documents medicaux » (module 2.1).
 */
public final class DocumentCommands {

    private DocumentCommands() {
    }

    /**
     * Televersement d'un document. La categorie est optionnelle : absente, elle
     * est determinee par la classification automatique (IA / regles + OCR).
     */
    public record UploadDocument(String title,
                                 MedicalDocument.DocumentCategory category,
                                 String originalFilename,
                                 String contentType,
                                 byte[] content,
                                 String dicomModality,
                                 String dicomStudyDescription) {
    }

    public record AddVersion(Long documentId,
                             String originalFilename,
                             String contentType,
                             byte[] content,
                             String changeNote) {
    }

    public record Reclassify(Long documentId, MedicalDocument.DocumentCategory category) {
    }

    public record ShareDocument(Long documentId,
                                Long granteeUserId,
                                MedicalDocument.SharePermission permission,
                                String reason,
                                Instant expiresAt) {
    }

    public record PushToDmp(Long documentId) {
    }
}
