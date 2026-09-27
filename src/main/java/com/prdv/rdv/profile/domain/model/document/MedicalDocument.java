package com.prdv.rdv.profile.domain.model.document;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agregat racine « Document medical » (module 2.1 — Documents Médicaux).
 *
 * <p>Couvre le televersement multi-format (PDF, JPG, PNG, DICOM), la
 * classification automatique (IA / regles), l'extraction OCR, le
 * versioning immuable, le partage securise avec des praticiens et la
 * transmission au DMP.
 *
 * <p>Le fichier lui-meme vit dans un stockage externe
 * ({@code MedicalFileStoragePort}) : l'agregat ne manipule que des cles de
 * stockage, des empreintes SHA-256 et des metadonnees.
 */
@Getter
@Setter
public class MedicalDocument {

    // ------------------------------------------------------------------
    // Enumerations
    // ------------------------------------------------------------------

    public enum DocumentCategory {
        PRESCRIPTION,
        LAB_RESULT,
        IMAGING_XRAY,
        IMAGING_MRI,
        IMAGING_CT_SCAN,
        IMAGING_ULTRASOUND,
        OPERATIVE_REPORT,
        HOSPITALIZATION_REPORT,
        CORRESPONDENCE,
        MEDICAL_CERTIFICATE,
        VACCINATION_RECORD,
        OTHER
    }

    public enum DocumentStatus { PENDING_CLASSIFICATION, CLASSIFIED, VALIDATED, ARCHIVED }

    public enum ClassificationSource { AUTOMATIC_AI, OCR_ASSISTED, USER_DEFINED, IMPORTED }

    public enum SharePermission { VIEW, VIEW_AND_DOWNLOAD }

    public enum OcrStatus { NOT_PROCESSED, SUCCEEDED, FAILED, UNSUPPORTED_FORMAT }

    // ------------------------------------------------------------------
    // Value Objects
    // ------------------------------------------------------------------

    /** Version immuable du document : le contenu n'est jamais ecrase. */
    public record DocumentVersion(int version,
                                  String storageKey,
                                  String originalFilename,
                                  String contentType,
                                  long sizeBytes,
                                  String sha256,
                                  String changeNote,
                                  Long uploadedBy,
                                  Instant uploadedAt) {

        public DocumentVersion {
            if (storageKey == null || storageKey.isBlank()) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La cle de stockage du document est obligatoire");
            }
            if (sizeBytes <= 0) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La taille du document doit etre strictement positive");
            }
        }
    }

    /** Partage securise : qui peut voir quoi, jusqu'a quand (granularite RGPD). */
    public record DocumentShare(String id,
                                Long granteeUserId,
                                SharePermission permission,
                                String reason,
                                Instant grantedAt,
                                Instant expiresAt,
                                Instant revokedAt) {

        public boolean isActive(Instant now) {
            return revokedAt == null
                    && granteeUserId != null
                    && (expiresAt == null || expiresAt.isAfter(now));
        }
    }

    /** Resultat d'extraction OCR : texte brut + champs structures. */
    public record OcrResult(OcrStatus status,
                            String rawText,
                            Map<String, String> extractedFields,
                            double confidence,
                            String engine,
                            Instant processedAt) {

        public OcrResult {
            extractedFields = extractedFields == null ? Map.of() : Map.copyOf(extractedFields);
            if (confidence < 0 || confidence > 1) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La confiance OCR doit etre comprise entre 0 et 1");
            }
        }

        public static OcrResult unsupported(String engine, Instant processedAt) {
            return new OcrResult(OcrStatus.UNSUPPORTED_FORMAT, null, Map.of(), 0, engine, processedAt);
        }
    }

    /** Decision de classification (automatique ou humaine) et sa provenance. */
    public record ClassificationResult(DocumentCategory category,
                                       double confidence,
                                       ClassificationSource source,
                                       String classifier,
                                       Instant classifiedAt) {

        public ClassificationResult {
            if (category == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La categorie du document est obligatoire");
            }
            if (confidence < 0 || confidence > 1) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "La confiance de classification doit etre comprise entre 0 et 1");
            }
        }
    }

    // ------------------------------------------------------------------
    // Etat de l'agregat
    // ------------------------------------------------------------------

    /** Seuil de confiance en dessous duquel la classification automatique reste a confirmer. */
    public static final double AUTO_CLASSIFICATION_TRUST = 0.60;

    private Long id;
    private Long ownerUserId;
    private String title;
    private DocumentCategory category;
    private DocumentStatus status;

    private ClassificationResult classification;
    private OcrResult ocrResult;

    /** Metadonnees DICOM (modality, description d'etude) lorsque le fichier est une image DICOM. */
    private String dicomModality;
    private String dicomStudyDescription;

    /** Transmis au DMP national : reference retournee par la passerelle. */
    private String dmpReference;
    private Instant dmpSharedAt;

    private List<DocumentVersion> versions = new ArrayList<>();
    private Set<DocumentShare> shares = new LinkedHashSet<>();

    private Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;

    // ------------------------------------------------------------------
    // Fabriques
    // ------------------------------------------------------------------

    public static MedicalDocument upload(Long ownerUserId,
                                         String title,
                                         DocumentCategory requestedCategory,
                                         String storageKey,
                                         String originalFilename,
                                         String contentType,
                                         long sizeBytes,
                                         String sha256,
                                         Long uploadedBy,
                                         Clock clock) {
        if (ownerUserId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un document medical doit appartenir a un patient");
        }
        MedicalDocument document = new MedicalDocument();
        document.ownerUserId = ownerUserId;
        document.title = title == null || title.isBlank()
                ? (originalFilename == null ? "Document medical" : originalFilename)
                : title.trim();
        document.createdAt = clock.instant();
        document.updatedAt = document.createdAt;
        document.versions.add(new DocumentVersion(1, storageKey, originalFilename, contentType,
                sizeBytes, sha256, "Depot initial", uploadedBy, clock.instant()));

        if (requestedCategory != null) {
            document.classification = new ClassificationResult(requestedCategory, 1.0,
                    ClassificationSource.USER_DEFINED, "patient", clock.instant());
            document.category = requestedCategory;
            document.status = DocumentStatus.CLASSIFIED;
        } else {
            document.status = DocumentStatus.PENDING_CLASSIFICATION;
        }
        return document;
    }

    // ------------------------------------------------------------------
    // Comportements : versioning
    // ------------------------------------------------------------------

    /** Ajoute une version ; les versions precedentes restent consultables (immutabilite). */
    public DocumentVersion addVersion(String storageKey, String originalFilename, String contentType,
                                      long sizeBytes, String sha256, String changeNote,
                                      Long uploadedBy, Clock clock) {
        if (status == DocumentStatus.ARCHIVED) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_IMMUTABLE,
                    "Un document archive ne peut plus recevoir de nouvelle version");
        }
        DocumentVersion version = new DocumentVersion(versions.size() + 1, storageKey, originalFilename,
                contentType, sizeBytes, sha256, changeNote, uploadedBy, clock.instant());
        this.versions.add(version);
        touch(clock);
        return version;
    }

    public DocumentVersion currentVersion() {
        if (versions.isEmpty()) {
            throw ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                    "Le document ne contient aucune version");
        }
        return versions.get(versions.size() - 1);
    }

    public int currentVersionNumber() {
        return versions.isEmpty() ? 0 : versions.size();
    }

    // ------------------------------------------------------------------
    // Comportements : classification / OCR / DICOM
    // ------------------------------------------------------------------

    public void applyClassification(ClassificationResult result, Clock clock) {
        this.classification = result;
        this.category = result.category();
        boolean trusted = result.source() == ClassificationSource.USER_DEFINED
                || result.confidence() >= AUTO_CLASSIFICATION_TRUST;
        this.status = trusted && status != DocumentStatus.VALIDATED
                ? DocumentStatus.CLASSIFIED
                : DocumentStatus.PENDING_CLASSIFICATION;
        touch(clock);
    }

    public void applyOcr(OcrResult result, Clock clock) {
        this.ocrResult = result;
        touch(clock);
    }

    public void recordDicomMetadata(String modality, String studyDescription, Clock clock) {
        this.dicomModality = modality;
        this.dicomStudyDescription = studyDescription;
        touch(clock);
    }

    public void validate(Clock clock) {
        if (category == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Impossible de valider un document non classe");
        }
        this.status = DocumentStatus.VALIDATED;
        touch(clock);
    }

    public void archive(Clock clock) {
        this.status = DocumentStatus.ARCHIVED;
        this.archivedAt = clock.instant();
        touch(clock);
    }

    public void markSharedWithDmp(String dmpReference, Clock clock) {
        this.dmpReference = dmpReference;
        this.dmpSharedAt = clock.instant();
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Comportements : partage
    // ------------------------------------------------------------------

    public DocumentShare shareWith(Long granteeUserId, SharePermission permission, String reason,
                                   Instant expiresAt, Clock clock) {
        if (granteeUserId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le destinataire du partage est obligatoire");
        }
        if (granteeUserId.equals(ownerUserId)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un patient n'a pas a se partager son propre document");
        }
        Instant now = clock.instant();
        boolean alreadyShared = shares.stream().anyMatch(share ->
                share.granteeUserId().equals(granteeUserId) && share.isActive(now));
        if (alreadyShared) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Ce document est deja partage avec cet utilisateur");
        }
        if (expiresAt != null && expiresAt.isBefore(now)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date d'expiration du partage est deja passee");
        }
        DocumentShare share = new DocumentShare(UUID.randomUUID().toString(), granteeUserId,
                permission == null ? SharePermission.VIEW : permission, reason, now, expiresAt, null);
        this.shares.add(share);
        touch(clock);
        return share;
    }

    public DocumentShare revokeShare(String shareId, Clock clock) {
        DocumentShare share = shares.stream()
                .filter(candidate -> candidate.id().equals(shareId))
                .findFirst()
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Aucun partage ne correspond a l'identifiant " + shareId));
        if (!share.isActive(clock.instant())) {
            throw ProfileException.of(ProfileErrorCode.SHARE_ALREADY_REVOKED, "Ce partage est deja clos");
        }
        DocumentShare revoked = new DocumentShare(share.id(), share.granteeUserId(), share.permission(),
                share.reason(), share.grantedAt(), share.expiresAt(), clock.instant());
        this.shares.remove(share);
        this.shares.add(revoked);
        touch(clock);
        return revoked;
    }

    public List<DocumentShare> activeShares(Clock clock) {
        Instant now = clock.instant();
        return shares.stream().filter(share -> share.isActive(now)).toList();
    }

    /**
     * Controle d'acces granulaire : le proprietaire, un destinataire d'un
     * partage actif, ou un utilisateur delegue par la plateforme.
     */
    public boolean isAccessibleBy(Long requesterUserId, Clock clock) {
        if (requesterUserId == null) {
            return false;
        }
        if (requesterUserId.equals(ownerUserId)) {
            return true;
        }
        return shares.stream().anyMatch(share -> share.isActive(clock.instant())
                && share.granteeUserId().equals(requesterUserId));
    }

    public boolean allowsDownload(Long requesterUserId, Clock clock) {
        if (requesterUserId != null && requesterUserId.equals(ownerUserId)) {
            return true;
        }
        Instant now = clock.instant();
        return shares.stream().anyMatch(share -> share.isActive(now)
                && share.granteeUserId().equals(requesterUserId)
                && share.permission() == SharePermission.VIEW_AND_DOWNLOAD);
    }

    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }
}
