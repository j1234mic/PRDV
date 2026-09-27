package com.prdv.rdv.profile.adapter.out.storage;

import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Stockage local des fichiers medicaux (developpement), sous
 * {@code prdv.profile.storage.local-path}.
 *
 * <p>En production, un adapteur S3/GCS avec chiffrement cote serveur
 * implemente le meme {@link MedicalFileStoragePort} : ni le domaine ni les
 * cas d'usage ne changent (Open/Closed, Dependency Inversion).
 *
 * <p>La resolution des cles interdit toute sortie du repertoire racine
 * (protection contre le path traversal).
 */
@Component
public class LocalMedicalFileStorageAdapter implements MedicalFileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalMedicalFileStorageAdapter.class);
    private static final int MAX_FILENAME_CHARS = 80;

    private final Path root;

    public LocalMedicalFileStorageAdapter(
            @Value("${prdv.profile.storage.local-path:./storage/profile}") String localPath) {
        this.root = Paths.get(localPath).toAbsolutePath().normalize();
        log.info("Stockage des fichiers medicaux : {}", root);
    }

    @Override
    public StoredFile store(String folder, String originalFilename, String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Fichier vide");
        }
        String key = sanitize(folder, "files") + "/" + UUID.randomUUID() + "-"
                + sanitize(originalFilename, "document");
        Path target = resolveInsideRoot(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("Echec du stockage du fichier medical [{}] : {}", key, e.getMessage());
            throw ProfileException.of(ProfileErrorCode.STORAGE_FAILURE,
                    "Le fichier medical n'a pas pu etre stocke");
        }
        log.debug("Fichier medical stocke : {} ({} octets, {})", key, content.length, contentType);
        return new StoredFile(key, content.length);
    }

    @Override
    public byte[] retrieve(String storageKey) {
        try {
            return Files.readAllBytes(resolveInsideRoot(storageKey));
        } catch (IOException e) {
            log.warn("Fichier medical illisible [{}] : {}", storageKey, e.getMessage());
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND, "Fichier introuvable");
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveInsideRoot(storageKey));
        } catch (IOException e) {
            log.warn("Fichier medical non supprime [{}] : {}", storageKey, e.getMessage());
        }
    }

    private Path resolveInsideRoot(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Cle de stockage absente");
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Cle de stockage invalide");
        }
        return target;
    }

    private static String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._/-]", "_").replaceAll("^\\.+", "");
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.length() > MAX_FILENAME_CHARS * 4
                ? cleaned.substring(cleaned.length() - MAX_FILENAME_CHARS * 4)
                : cleaned;
    }
}
