package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/**
 * Cas d'usage : cycle de vie des documents medicaux
 * (televersement, classification, OCR, versioning).
 */
public interface MedicalDocumentUseCase {

    /** Televersement : stockage, empreinte SHA-256, classification IA + OCR. */
    ProfileViews.MedicalDocumentView upload(DocumentCommands.UploadDocument command);

    /** Nouvelle version : le document precedent reste consultable. */
    ProfileViews.MedicalDocumentView addVersion(DocumentCommands.AddVersion command);

    /** Requalification manuelle (la classification automatique peut etre corrigee). */
    ProfileViews.MedicalDocumentView reclassify(DocumentCommands.Reclassify command);

    void archive(Long documentId);
}
