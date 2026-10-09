package com.prdv.rdv.profile.adapter.out.storage;

import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chiffrement au repos des fichiers medicaux : aucune donnee en clair sur le support. */
class EncryptedMedicalFileStorageAdapterTest {

    private static final String KEY = "test-profile-storage-key-0123456789";
    private static final byte[] PLAIN = "Compte rendu : tension 13/8, rythme sinusal"
            .getBytes(StandardCharsets.UTF_8);

    @Test
    @DisplayName("Le fichier est chiffre sur le support et restitue a l'identique")
    void roundTripEncryptsAtRestAndRestoresContent() {
        InMemoryStorage delegate = new InMemoryStorage();
        EncryptedMedicalFileStorageAdapter adapter = new EncryptedMedicalFileStorageAdapter(delegate, KEY);

        MedicalFileStoragePort.StoredFile stored = adapter.store("medical-documents/7", "cr.pdf",
                "application/pdf", PLAIN);

        byte[] onDisk = delegate.read(stored.storageKey());
        assertThat(new String(onDisk, 0, 8, StandardCharsets.US_ASCII)).isEqualTo("PRDVENC1");
        assertThat(new String(onDisk, StandardCharsets.UTF_8)).doesNotContain("tension");
        assertThat(adapter.retrieve(stored.storageKey())).isEqualTo(PLAIN);
    }

    @Test
    @DisplayName("Chaque ecriture utilise un IV neuf : le meme contenu ne produit pas le meme chiffre")
    void eachStoreUsesAFreshIv() {
        InMemoryStorage delegate = new InMemoryStorage();
        EncryptedMedicalFileStorageAdapter adapter = new EncryptedMedicalFileStorageAdapter(delegate, KEY);

        MedicalFileStoragePort.StoredFile first = adapter.store("f", "a.pdf", "application/pdf", PLAIN);
        MedicalFileStoragePort.StoredFile second = adapter.store("f", "a.pdf", "application/pdf", PLAIN);

        assertThat(delegate.read(first.storageKey())).isNotEqualTo(delegate.read(second.storageKey()));
    }

    @Test
    @DisplayName("Un contenu altere est detecte a la lecture (GCM authentifie)")
    void tamperedCiphertextIsRejected() {
        InMemoryStorage delegate = new InMemoryStorage();
        EncryptedMedicalFileStorageAdapter adapter = new EncryptedMedicalFileStorageAdapter(delegate, KEY);
        MedicalFileStoragePort.StoredFile stored = adapter.store("f", "a.pdf", "application/pdf", PLAIN);

        byte[] tampered = delegate.read(stored.storageKey());
        tampered[tampered.length - 1] ^= 0x01;
        delegate.write(stored.storageKey(), tampered);

        assertThatThrownBy(() -> adapter.retrieve(stored.storageKey()))
                .isInstanceOf(ProfileException.class)
                .hasMessageContaining("illisible");
    }

    @Test
    @DisplayName("Une autre cle ne permet pas de lire le fichier")
    void wrongKeyCannotOpenFile() {
        InMemoryStorage delegate = new InMemoryStorage();
        MedicalFileStoragePort.StoredFile stored = new EncryptedMedicalFileStorageAdapter(delegate, KEY)
                .store("f", "a.pdf", "application/pdf", PLAIN);

        EncryptedMedicalFileStorageAdapter other = new EncryptedMedicalFileStorageAdapter(delegate,
                "une-autre-cle-de-test-0123456789");

        assertThatThrownBy(() -> other.retrieve(stored.storageKey())).isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("Un fichier anterieur au chiffrement (sans en-tete) est restitue tel quel")
    void legacyPlaintextFilesAreReturnedUnchanged() {
        InMemoryStorage delegate = new InMemoryStorage();
        String legacyKey = "legacy/" + UUID.randomUUID() + "-ancien.pdf";
        delegate.write(legacyKey, PLAIN);

        EncryptedMedicalFileStorageAdapter adapter = new EncryptedMedicalFileStorageAdapter(delegate, KEY);

        assertThat(adapter.retrieve(legacyKey)).isEqualTo(PLAIN);
    }

    @Test
    @DisplayName("L'absence de cle de chiffrement empeche le demarrage (fail fast)")
    void missingKeyFailsFastAtStartup() {
        assertThatThrownBy(() -> new EncryptedMedicalFileStorageAdapter(new InMemoryStorage(), "   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("encryption-key");
    }

    /** Faux delegue en memoire : enregistre les octets tels qu'ils seraient poses sur le support. */
    private static final class InMemoryStorage implements MedicalFileStoragePort {

        private final Map<String, byte[]> files = new HashMap<>();

        @Override
        public StoredFile store(String folder, String originalFilename, String contentType, byte[] content) {
            String key = folder + "/" + UUID.randomUUID() + "-" + originalFilename;
            files.put(key, content.clone());
            return new StoredFile(key, content.length);
        }

        @Override
        public byte[] retrieve(String storageKey) {
            byte[] content = files.get(storageKey);
            if (content == null) {
                throw ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Fichier introuvable : " + storageKey);
            }
            return content.clone();
        }

        @Override
        public void delete(String storageKey) {
            files.remove(storageKey);
        }

        byte[] read(String storageKey) {
            return files.get(storageKey).clone();
        }

        void write(String storageKey, byte[] content) {
            files.put(storageKey, content.clone());
        }
    }
}
