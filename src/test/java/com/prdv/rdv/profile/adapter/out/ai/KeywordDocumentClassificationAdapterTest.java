package com.prdv.rdv.profile.adapter.out.ai;

import com.prdv.rdv.profile.application.port.output.DocumentClassificationPort;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Classification par referentiel de mots-cles (Strategy) : le lexique pondere
 * doit repondre aux cas typiques du module 2.1 et rester defensif (OTHER a
 * faible confiance quand rien ne discriminate).
 */
class KeywordDocumentClassificationAdapterTest {

    private final KeywordDocumentClassificationAdapter adapter = new KeywordDocumentClassificationAdapter();

    @Test
    @DisplayName("Une ordonnance est classee PRESCRIPTION avec une confiance exploitable")
    void classifiesPrescription() {
        DocumentClassificationPort.Classification result = adapter.classify(
                request("ordonnance-dr-martin.pdf", "application/pdf", null, null));

        assertThat(result.category()).isEqualTo(MedicalDocument.DocumentCategory.PRESCRIPTION);
        assertThat(result.confidence()).isGreaterThanOrEqualTo(MedicalDocument.AUTO_CLASSIFICATION_TRUST);
    }

    @Test
    @DisplayName("Un compte rendu de biologie est classee LAB_RESULT")
    void classifiesLabResultFromOcrText() {
        DocumentClassificationPort.Classification result = adapter.classify(
                request("courrier.pdf", "application/pdf",
                        "Resultats du laboratoire : glycemie 1,08 g/l - hemoglobine 14,2", null));

        assertThat(result.category()).isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
    }

    @Test
    @DisplayName("Les metadonnées DICOM orientent l'imagerie")
    void classifiesDicomFromModality() {
        DocumentClassificationPort.Classification result = adapter.classify(
                request("serie.dcm", "application/dicom", null, "ct"));

        assertThat(result.category()).isEqualTo(MedicalDocument.DocumentCategory.IMAGING_CT_SCAN);
    }

    @Test
    @DisplayName("Sans aucun signal, la categorie OTHER est renvoyee a faible confiance")
    void fallsBackToOther() {
        DocumentClassificationPort.Classification result = adapter.classify(
                request("scan0001", null, null, null));

        assertThat(result.category()).isEqualTo(MedicalDocument.DocumentCategory.OTHER);
        assertThat(result.confidence()).isLessThan(MedicalDocument.AUTO_CLASSIFICATION_TRUST);
    }

    @Test
    @DisplayName("Le nom de l'implementation est expose pour la tracabilite")
    void exposesClassifierName() {
        assertThat(adapter.name()).isEqualTo("keyword-lexicon-v1");
    }

    private static DocumentClassificationPort.ClassificationRequest request(
            String filename, String contentType, String extractedText, String dicomModality) {
        return new DocumentClassificationPort.ClassificationRequest(filename, contentType,
                extractedText, dicomModality, new byte[]{1});
    }
}
