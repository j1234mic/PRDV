package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;

/**
 * Port de sortie : classification automatique d'un document medical
 * (Strategy).
 *
 * <p>L'adapteur fourni applique un referentiel de mots-cles ; un adapteur
 * d'IA (modele de vision, NLP medical) peut le remplacer sans aucune
 * modification des cas d'usage (Open/Closed, Dependency Inversion).
 */
public interface DocumentClassificationPort {

    Classification classify(ClassificationRequest request);

    /** Nom de l'implémentation, trace dans l'audit et la vue du document. */
    String name();

    record ClassificationRequest(String filename,
                                 String contentType,
                                 String extractedText,
                                 String dicomModality,
                                 byte[] content) {
    }

    record Classification(MedicalDocument.DocumentCategory category, double confidence) {
    }
}
