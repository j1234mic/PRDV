package com.prdv.rdv.iam.adapter.out.storage;

import com.prdv.rdv.iam.application.port.output.DocumentStoragePort;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Stockage des pieces justificatives KYC sur le disque local (developpement),
 * sous {@code prdv.storage.local-path} ({@code ./storage/kyc} par defaut).
 *
 * <p>Le fichier lui-meme n'est jamais persiste en base : seule la cle de
 * stockage retournee ici est conservee dans {@code KycDocument}. En production,
 * un adapteur S3/GCS implemente le meme {@link DocumentStoragePort} sans aucun
 * changement cote domaine ni cote cas d'usage (Open/Closed, Dependency Inversion).
 */
@Component
public class LocalDocumentStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalDocumentStorageAdapter.class);

    /** Longueur maximale conservee pour le nom de fichier d'origine. */
    private static final int MAX_FILENAME_CHARS = 80;

    private final Path root;

    public LocalDocumentStorageAdapter(IamProperties properties) {
        this.root = Paths.get(properties.getStorage().getLocalPath()).toAbsolutePath().normalize();
    }

    @Override
    public StoredDocument store(String ownerContext, String originalFilename,
                                String contentType, byte[] content) {
        if (content == null || content.length == 0) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Fichier vide");
        }
        String key = sanitize(ownerContext, "owner") + "/"
                + UUID.randomUUID() + "-" + sanitize(originalFilename, "document");
        Path target = resolveInsideRoot(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            log.error("Echec du stockage KYC [{}] : {}", key, e.getMessage());
            throw IamException.of(IamErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Stockage du document impossible");
        }
        log.debug("Document KYC stocke : {} ({} octets, type {})", key, content.length, contentType);
        return new DocumentStoragePort.StoredDocument(key, content.length);
    }

    @Override
    public byte[] retrieve(String storageKey) {
        Path target = resolveInsideRoot(storageKey);
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            log.warn("Document KYC illisible [{}] : {}", storageKey, e.getMessage());
            throw IamException.of(IamErrorCode.NOT_FOUND, "Document introuvable");
        }
    }

    /**
     * Resout la cle dans le repertoire racine en interdisant toute sortie
     * (path traversal : {@code ../}, chemins absolus, separateurs exotiques).
     */
    private Path resolveInsideRoot(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Cle de stockage absente");
        }
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Cle de stockage invalide");
        }
        return target;
    }

    /** N'autorise que {@code [A-Za-z0-9._-]} ; conserve la fin (extension). */
    private static String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String cleaned = value.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("^\\.+", "");
        if (cleaned.isBlank()) {
            return fallback;
        }
        return cleaned.length() > MAX_FILENAME_CHARS
                ? cleaned.substring(cleaned.length() - MAX_FILENAME_CHARS)
                : cleaned;
    }
}
