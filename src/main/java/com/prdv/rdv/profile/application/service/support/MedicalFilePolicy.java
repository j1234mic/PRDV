package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;

/**
 * Politique d'acceptation des fichiers medicaux (Single Responsibility) :
 * formats autorises, taille maximale, empreinte SHA-256 et detection DICOM.
 *
 * <p>Isoler ces regles permet de les faire evoluer (ajout du JPEG 2000,
 * analyse antivirus, DLP) sans toucher aux cas d'usage.
 */
@Component
public class MedicalFilePolicy {

    /** Formats acceptes pour les documents medicaux (PDF, images, DICOM, textes). */
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/tiff",
            "image/heic",
            "application/dicom",
            "application/octet-stream",
            "text/plain",
            "text/csv",
            "text/xml",
            "application/xml");

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic");

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/quicktime", "video/webm");

    /** Signature du format DICOM : « DICM » en position 128. */
    private static final byte[] DICOM_MAGIC = {'D', 'I', 'C', 'M'};
    private static final int DICOM_MAGIC_OFFSET = 128;

    public void validateDocument(String contentType, byte[] content, long maxBytes) {
        requireContent(content);
        if (content.length > maxBytes) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_TOO_LARGE,
                    "Document trop volumineux : " + content.length + " octets (maximum " + maxBytes + ")");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT).trim();
        if (!DOCUMENT_TYPES.contains(normalized)) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED,
                    "Format non pris en charge : " + contentType
                            + " (formats acceptes : PDF, JPEG, PNG, TIFF, DICOM, texte)");
        }
    }

    public void validateImage(String contentType, byte[] content, long maxBytes) {
        requireContent(content);
        if (content.length > maxBytes) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_TOO_LARGE,
                    "Image trop volumineuse (maximum " + maxBytes + " octets)");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT).trim();
        if (!IMAGE_TYPES.contains(normalized)) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED,
                    "Format d'image non pris en charge : " + contentType);
        }
    }

    public void validateVideo(String contentType, byte[] content, long maxBytes) {
        requireContent(content);
        if (content.length > maxBytes) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_TOO_LARGE,
                    "Video trop volumineuse (maximum " + maxBytes + " octets)");
        }
        String normalized = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT).trim();
        if (!VIDEO_TYPES.contains(normalized)) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED,
                    "Format de video non pris en charge : " + contentType);
        }
    }

    /** Detecte un fichier DICOM reel (preambule 128 octets + « DICM »). */
    public boolean isDicom(String contentType, byte[] content) {
        if (content == null || content.length < DICOM_MAGIC_OFFSET + DICOM_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < DICOM_MAGIC.length; i++) {
            if (content[DICOM_MAGIC_OFFSET + i] != DICOM_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /** Empreinte SHA-256 hexadecimale : garantit l'integrite et detecte les doublons. */
    public String sha256(byte[] content) {
        requireContent(content);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible dans ce JDK", e);
        }
    }

    /** Texte brut pour les fichiers deja textuels (alimente l'OCR sans moteur externe). */
    public String asText(String contentType, byte[] content) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(java.util.Locale.ROOT).trim();
        if (normalized.startsWith("text/") || normalized.equals("application/xml")
                || normalized.equals("application/csv")) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

    private void requireContent(byte[] content) {
        if (content == null || content.length == 0) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Fichier vide");
        }
    }
}
