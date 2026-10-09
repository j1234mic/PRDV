package com.prdv.rdv.profile.application.service.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Type MIME et nom d'origine deduits de la cle de stockage (le domaine ne garde pas le type d'origine). */
class MediaContentTypesTest {

    private static final String UUID_PREFIX = "3f2a1c3e-4b5d-4e6f-8a9b-0c1d2e3f4a5b-";

    @Test
    @DisplayName("Le type MIME suit l'extension, sans tenir compte de la casse")
    void contentTypeFollowsExtension() {
        assertThat(MediaContentTypes.contentTypeOf("practitioner-media/7/photo/" + UUID_PREFIX + "portrait.JPG"))
                .isEqualTo("image/jpeg");
        assertThat(MediaContentTypes.contentTypeOf("patient-media/7/identity/" + UUID_PREFIX + "carte.pdf"))
                .isEqualTo("application/pdf");
        assertThat(MediaContentTypes.contentTypeOf("practitioner-media/7/video/" + UUID_PREFIX + "presentation.mp4"))
                .isEqualTo("video/mp4");
    }

    @Test
    @DisplayName("Une extension inconnue ou absente retombe sur application/octet-stream")
    void unknownExtensionFallsBackToOctetStream() {
        assertThat(MediaContentTypes.contentTypeOf("files/" + UUID_PREFIX + "donnees.xyz"))
                .isEqualTo(MediaContentTypes.OCTET_STREAM);
        assertThat(MediaContentTypes.contentTypeOf("files/" + UUID_PREFIX + "sans-extension"))
                .isEqualTo(MediaContentTypes.OCTET_STREAM);
    }

    @Test
    @DisplayName("Le nom d'origine retire le dossier et le prefixe UUID")
    void filenameStripsFolderAndUuidPrefix() {
        assertThat(MediaContentTypes.filenameOf("patient-media/7/identity/" + UUID_PREFIX + "carte-identite.pdf"))
                .isEqualTo("carte-identite.pdf");
        assertThat(MediaContentTypes.filenameOf("no-uuid/plain.txt")).isEqualTo("plain.txt");
        assertThat(MediaContentTypes.filenameOf(null)).isEqualTo("media");
    }
}
