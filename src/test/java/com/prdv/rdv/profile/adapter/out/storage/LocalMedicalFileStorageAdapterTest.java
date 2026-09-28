package com.prdv.rdv.profile.adapter.out.storage;

import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stockage local des fichiers medicaux : aller-retour fiable et protection
 * contre le path traversal.
 */
class LocalMedicalFileStorageAdapterTest {

    @TempDir
    private Path storageRoot;

    @Test
    @DisplayName("Un fichier stocke peut etre relu integralement")
    void storesAndRetrieves() {
        LocalMedicalFileStorageAdapter adapter = new LocalMedicalFileStorageAdapter(
                storageRoot.toString());
        byte[] content = "contenu medical".getBytes(StandardCharsets.UTF_8);

        MedicalFileStoragePort.StoredFile stored = adapter.store("medical-documents/42",
                "analyses.pdf", "application/pdf", content);

        assertThat(stored.sizeBytes()).isEqualTo(content.length);
        assertThat(stored.storageKey()).contains("medical-documents/42").contains("analyses.pdf");
        assertThat(adapter.retrieve(stored.storageKey())).isEqualTo(content);
    }

    @Test
    @DisplayName("Un fichier vide est refuse")
    void rejectsEmptyContent() {
        LocalMedicalFileStorageAdapter adapter = new LocalMedicalFileStorageAdapter(
                storageRoot.toString());

        assertThatThrownBy(() -> adapter.store("folder", "a.pdf", "application/pdf", new byte[0]))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Une cle tentant de sortir du repertoire racine est refusee")
    void rejectsPathTraversal() {
        LocalMedicalFileStorageAdapter adapter = new LocalMedicalFileStorageAdapter(
                storageRoot.toString());

        assertThatThrownBy(() -> adapter.retrieve("../../etc/passwd"))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("La suppression est idempotente")
    void deleteIsIdempotent() {
        LocalMedicalFileStorageAdapter adapter = new LocalMedicalFileStorageAdapter(
                storageRoot.toString());
        MedicalFileStoragePort.StoredFile stored = adapter.store("folder", "a.pdf",
                "application/pdf", new byte[]{1});

        adapter.delete(stored.storageKey());
        adapter.delete(stored.storageKey());

        assertThatThrownBy(() -> adapter.retrieve(stored.storageKey()))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_NOT_FOUND);
    }
}
