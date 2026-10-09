package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.application.result.ProfileViews;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/**
 * Restitution HTTP commune des fichiers (documents, photos, scans, videos) :
 * type MIME, taille et nom de fichier restitues a l'identique du stockage.
 */
final class FileResponses {

    private FileResponses() {
    }

    /**
     * @param file       fichier deja controle par le cas d'usage
     * @param attachment {@code true} : telechargement ; {@code false} : affichage (photo, video)
     */
    static ResponseEntity<byte[]> of(ProfileViews.DocumentFile file, boolean attachment) {
        ContentDisposition disposition = (attachment ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(file.contentType() == null
                        ? MediaType.APPLICATION_OCTET_STREAM_VALUE : file.contentType()))
                .contentLength(file.sizeBytes())
                .body(file.content());
    }
}
