package com.prdv.rdv.profile.domain.model.document;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documents medicaux : versioning immutable, classification, partage securise
 * (revoquable, bornable dans le temps, trace conservee).
 */
class MedicalDocumentVersioningTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long PRACTITIONER = 7L;

    @Test
    @DisplayName("Un depot sans categorie attend la classification automatique")
    void uploadWithoutCategoryWaitsForClassification() {
        MedicalDocument document = upload(null);

        assertThat(document.currentVersionNumber()).isEqualTo(1);
        assertThat(document.getStatus()).isEqualTo(MedicalDocument.DocumentStatus.PENDING_CLASSIFICATION);
        assertThat(document.getCategory()).isNull();
    }

    @Test
    @DisplayName("Une categorie demandee par le patient est tracee comme USER_DEFINED")
    void userDefinedCategoryIsRecorded() {
        MedicalDocument document = upload(MedicalDocument.DocumentCategory.PRESCRIPTION);

        assertThat(document.getStatus()).isEqualTo(MedicalDocument.DocumentStatus.CLASSIFIED);
        assertThat(document.getClassification().source())
                .isEqualTo(MedicalDocument.ClassificationSource.USER_DEFINED);
        assertThat(document.getClassification().confidence()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Les versions s'empilent sans ecraser les precedentes")
    void versionsAccumulate() {
        MedicalDocument document = upload(null);

        document.addVersion("docs/42/v2.pdf", "ordonnance-v2.pdf", "application/pdf", 2_048,
                "sha256-v2", "Correction du dosage", PATIENT, CLOCK);

        assertThat(document.currentVersionNumber()).isEqualTo(2);
        assertThat(document.getVersions()).hasSize(2);
        assertThat(document.currentVersion().changeNote()).isEqualTo("Correction du dosage");
        assertThat(document.getVersions().get(0).sha256()).isEqualTo("sha256-v1");
    }

    @Test
    @DisplayName("Un document archive est immutable")
    void archivedDocumentIsImmutable() {
        MedicalDocument document = upload(null);
        document.applyClassification(new MedicalDocument.ClassificationResult(
                MedicalDocument.DocumentCategory.LAB_RESULT, 0.93,
                MedicalDocument.ClassificationSource.AUTOMATIC_AI, "keyword-classifier", CLOCK.instant()),
                CLOCK);
        document.archive(CLOCK);

        assertThat(document.getStatus()).isEqualTo(MedicalDocument.DocumentStatus.ARCHIVED);
        assertThatThrownBy(() -> document.addVersion("docs/42/v2.pdf", "v2.pdf", "application/pdf", 10,
                "sha256-v2", "Tentative", PATIENT, CLOCK))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_IMMUTABLE);
    }

    @Test
    @DisplayName("Un partage actif ouvre la lecture, pas le telechargement sans droit explicite")
    void shareGrantsReadAndOptionallyDownload() {
        MedicalDocument document = upload(null);

        MedicalDocument.DocumentShare view = document.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Suivi cardiologique", null, CLOCK);

        assertThat(view.isActive(CLOCK.instant())).isTrue();
        assertThat(document.isAccessibleBy(PRACTITIONER, CLOCK)).isTrue();
        assertThat(document.allowsDownload(PRACTITIONER, CLOCK)).isFalse();
        assertThat(document.isAccessibleBy(99L, CLOCK)).isFalse();

        document.revokeShare(view.id(), CLOCK);
        MedicalDocument.DocumentShare download = document.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD, "Avis specialise", null, CLOCK);

        assertThat(document.allowsDownload(PRACTITIONER, CLOCK)).isTrue();
        assertThat(document.getShares()).hasSize(2);
        assertThat(download.isActive(CLOCK.instant())).isTrue();
    }

    @Test
    @DisplayName("La revocation laisse une trace et ne peut pas etre rejouee")
    void revocationKeepsTrace() {
        MedicalDocument document = upload(null);
        MedicalDocument.DocumentShare share = document.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Suivi", null, CLOCK);

        MedicalDocument.DocumentShare revoked = document.revokeShare(share.id(), CLOCK);

        assertThat(revoked.revokedAt()).isEqualTo(CLOCK.instant());
        assertThat(revoked.id()).isEqualTo(share.id());
        assertThat(document.getShares()).hasSize(1);
        assertThat(document.activeShares(CLOCK)).isEmpty();
        assertThat(document.isAccessibleBy(PRACTITIONER, CLOCK)).isFalse();

        assertThatThrownBy(() -> document.revokeShare(share.id(), CLOCK))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.SHARE_ALREADY_REVOKED);
    }

    @Test
    @DisplayName("Un partage expire cesse de donner acces sans intervention")
    void expiredShareStopsAccess() {
        MedicalDocument document = upload(null);
        Instant expiry = CLOCK.instant().plus(Duration.ofDays(7));
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD, "Avis ponctuel",
                expiry, CLOCK);

        assertThat(document.isAccessibleBy(PRACTITIONER, CLOCK)).isTrue();

        Clock afterExpiry = Clock.fixed(expiry.plus(Duration.ofHours(1)), ZoneOffset.UTC);
        assertThat(document.isAccessibleBy(PRACTITIONER, afterExpiry)).isFalse();
        assertThat(document.allowsDownload(PRACTITIONER, afterExpiry)).isFalse();
    }

    @Test
    @DisplayName("Un patient ne peut pas se partager son propre document, ni partager dans le passe")
    void invalidSharesAreRejected() {
        MedicalDocument document = upload(null);

        assertThatThrownBy(() -> document.shareWith(PATIENT, MedicalDocument.SharePermission.VIEW, "Soi",
                null, CLOCK))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW,
                "Expire", CLOCK.instant().minus(Duration.ofDays(1)), CLOCK))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("La transmission au DMP est referencee et datee")
    void dmpPublicationIsReferenced() {
        MedicalDocument document = upload(null);

        document.markSharedWithDmp("DMPDOC-1234", CLOCK);

        assertThat(document.getDmpReference()).isEqualTo("DMPDOC-1234");
        assertThat(document.getDmpSharedAt()).isEqualTo(CLOCK.instant());
    }

    private static MedicalDocument upload(MedicalDocument.DocumentCategory category) {
        return MedicalDocument.upload(PATIENT, "Ordonnance", category, "docs/42/v1.pdf",
                "ordonnance.pdf", "application/pdf", 1_024, "sha256-v1", PATIENT, CLOCK);
    }
}
