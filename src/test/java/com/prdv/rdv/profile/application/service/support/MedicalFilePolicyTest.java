package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Politique d'acceptation des fichiers medicaux : formats, taille, empreinte
 * SHA-256, detection DICOM et extraction texte.
 */
class MedicalFilePolicyTest {

    private static final long MAX_BYTES = 1_024;

    private final MedicalFilePolicy policy = new MedicalFilePolicy();

    @Test
    @DisplayName("Les formats medicaux attendus sont acceptes")
    void acceptsMedicalFormats() {
        for (String contentType : new String[]{"application/pdf", "image/jpeg", "image/png",
                "image/tiff", "application/dicom", "text/plain", "text/csv", "text/xml"}) {
            policy.validateDocument(contentType, new byte[]{1, 2, 3}, MAX_BYTES);
        }
    }

    @Test
    @DisplayName("Un format inconnu est refuse avec DOCUMENT_FORMAT_UNSUPPORTED")
    void rejectsUnsupportedFormat() {
        assertThatThrownBy(() -> policy.validateDocument("application/x-msdownload",
                new byte[]{1}, MAX_BYTES))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED);
    }

    @Test
    @DisplayName("Un document depassant la taille maximale est refuse avec DOCUMENT_TOO_LARGE")
    void rejectsTooLargeDocument() {
        byte[] content = new byte[(int) MAX_BYTES + 1];
        assertThatThrownBy(() -> policy.validateDocument("application/pdf", content, MAX_BYTES))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_TOO_LARGE);
    }

    @Test
    @DisplayName("Un fichier vide est refuse")
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> policy.validateDocument("application/pdf", new byte[0], MAX_BYTES))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("La signature DICM en position 128 identifie un vrai fichier DICOM")
    void detectsDicomSignature() {
        byte[] dicom = new byte[132];
        Arrays.fill(dicom, (byte) 0);
        dicom[128] = 'D';
        dicom[129] = 'I';
        dicom[130] = 'C';
        dicom[131] = 'M';

        assertThat(policy.isDicom("application/dicom", dicom)).isTrue();
        assertThat(policy.isDicom("application/pdf", new byte[200])).isFalse();
        assertThat(policy.isDicom("application/dicom", new byte[10])).isFalse();
        assertThat(policy.isDicom("application/dicom", null)).isFalse();
    }

    @Test
    @DisplayName("L'empreinte SHA-256 est stable et hexadecimale")
    void computesSha256() {
        // Vecteur de reference : SHA-256(\"abc\")
        assertThat(policy.sha256("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("Seuls les fichiers textuels sont exposes a l'OCR")
    void asTextOnlyForTextualContent() {
        assertThat(policy.asText("text/plain", "Bonjour".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("Bonjour");
        assertThat(policy.asText("application/pdf", new byte[]{1})).isNull();
    }

    @Test
    @DisplayName("Les images valident par la politique dediee")
    void validatesImagesSeparately() {
        policy.validateImage("image/png", new byte[]{1}, MAX_BYTES);
        assertThatThrownBy(() -> policy.validateImage("application/pdf", new byte[]{1}, MAX_BYTES))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED);
    }
}
