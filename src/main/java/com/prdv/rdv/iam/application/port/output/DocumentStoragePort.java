package com.prdv.rdv.iam.application.port.output;

/**
 * Stockage des pieces justatives KYC.
 * Implementation disque local en dev ; adapteur S3/GCS en production.
 */
public interface DocumentStoragePort {

    StoredDocument store(String ownerContext, String originalFilename, String contentType, byte[] content);

    byte[] retrieve(String storageKey);

    record StoredDocument(String storageKey, long sizeBytes) {
    }
}
