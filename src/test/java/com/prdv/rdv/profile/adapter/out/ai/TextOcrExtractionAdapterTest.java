package com.prdv.rdv.profile.adapter.out.ai;

import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extraction OCR des fichiers textuels : champs structures reglementaires
 * (NIR, dates, identifiants d'acte, valeurs biologiques) et formats non
 * pris en charge declares honnetement.
 */
class TextOcrExtractionAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"),
            ZoneOffset.UTC);

    private final TextOcrExtractionAdapter adapter =
            new TextOcrExtractionAdapter(new MedicalFilePolicy(), CLOCK);

    @Test
    @DisplayName("Un fichier texte expose NIR, date, identifiant praticien et valeurs biologiques")
    void extractsStructuredFields() {
        String text = """
                Compte rendu de biologie du 12/09/2026
                Patient : 1 85 03 25 108 034 42
                Practicien RPPS : 10003456789
                glycemie : 1,08 g/l - cholesterol = 5,2
                """;

        MedicalDocument.OcrResult result = adapter.extract("analyses.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));

        assertThat(result.status()).isEqualTo(MedicalDocument.OcrStatus.SUCCEEDED);
        assertThat(result.extractedFields())
                .containsEntry("socialSecurityNumber", "185032510803442")
                .containsEntry("documentDate", "12/09/2026")
                .containsEntry("practitionerIdentifier", "10003456789")
                .containsEntry("lab_glycemie", "1,08")
                .containsEntry("lab_cholesterol", "5,2");
        assertThat(result.confidence()).isEqualTo(0.75);
        assertThat(result.engine()).isEqualTo("text-extractor-v1");
    }

    @Test
    @DisplayName("Une image est declaree non prise en charge (un moteur externe est requis)")
    void declaresImagesUnsupported() {
        MedicalDocument.OcrResult result = adapter.extract("radio.png", "image/png", new byte[]{1});

        assertThat(result.status()).isEqualTo(MedicalDocument.OcrStatus.UNSUPPORTED_FORMAT);
    }

    @Test
    @DisplayName("Un texte sans champ structure reste une extraction a faible confiance")
    void textWithoutFieldsHasLowConfidence() {
        MedicalDocument.OcrResult result = adapter.extract("note.txt", "text/plain",
                "Rendez-vous de controle habituel.".getBytes(StandardCharsets.UTF_8));

        assertThat(result.status()).isEqualTo(MedicalDocument.OcrStatus.SUCCEEDED);
        assertThat(result.extractedFields()).isEmpty();
        assertThat(result.confidence()).isEqualTo(0.35);
    }
}
