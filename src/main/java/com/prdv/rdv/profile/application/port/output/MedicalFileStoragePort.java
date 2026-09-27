package com.prdv.rdv.profile.application.port.output;

/**
 * Port de sortie : stockage des fichiers medicaux (documents, imagerie,
 * photos de profil et de cabinet).
 *
 * <p>Adapteurs : disque local en developpement, S3/GCS chiffre en production.
 * Le domaine et les cas d'usage ne manipulent que des cles de stockage.
 */
public interface MedicalFileStoragePort {

    StoredFile store(String folder, String originalFilename, String contentType, byte[] content);

    byte[] retrieve(String storageKey);

    void delete(String storageKey);

    record StoredFile(String storageKey, long sizeBytes) {
    }
}
