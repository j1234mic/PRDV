package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.mapper.MedicalPersistenceMapper;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration JPA des documents medicaux : aller-retour domaine &lt;-&gt; base
 * (versions JSON, classification, OCR, partages dans leur table dediee) et
 * requetes d'acces interrogeables.
 */
@DataJpaTest
@Import({MedicalDocumentPersistenceAdapter.class, MedicalPersistenceMapper.class})
class MedicalDocumentPersistenceAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"),
            ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long PRACTITIONER = 7L;

    @Autowired
    private MedicalDocumentRepository repository;

    @Test
    @DisplayName("Un document complet survit a un aller-retour base de donnees")
    void saveAndReloadPreservesWholeAggregate() {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Analyses du 12/09", null,
                "medical-documents/42/a.pdf", "analyses.pdf", "application/pdf", 2_048,
                "sha256-v1", PATIENT, CLOCK);
        document.applyOcr(new MedicalDocument.OcrResult(MedicalDocument.OcrStatus.SUCCEEDED,
                "glycemie 1,08", Map.of("lab_glycemie", "1,08"), 0.75, "text-extractor-v1",
                CLOCK.instant()), CLOCK);
        document.applyClassification(new MedicalDocument.ClassificationResult(
                MedicalDocument.DocumentCategory.LAB_RESULT, 0.9,
                MedicalDocument.ClassificationSource.AUTOMATIC_AI, "keyword-lexicon-v1",
                CLOCK.instant()), CLOCK);
        document.recordDicomMetadata("OT", "Etude annexe", CLOCK);
        document.markSharedWithDmp("DMPDOC-1", CLOCK);
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD,
                "Avis", null, CLOCK);

        MedicalDocument saved = repository.save(document);
        assertThat(saved.getId()).isNotNull();

        MedicalDocument reloaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getTitle()).isEqualTo("Analyses du 12/09");
        assertThat(reloaded.getCategory()).isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
        assertThat(reloaded.getStatus()).isEqualTo(MedicalDocument.DocumentStatus.CLASSIFIED);
        assertThat(reloaded.getClassification().source())
                .isEqualTo(MedicalDocument.ClassificationSource.AUTOMATIC_AI);
        assertThat(reloaded.getClassification().confidence()).isEqualTo(0.9);
        assertThat(reloaded.getOcrResult().extractedFields()).containsEntry("lab_glycemie", "1,08");
        assertThat(reloaded.getDicomModality()).isEqualTo("OT");
        assertThat(reloaded.getDmpReference()).isEqualTo("DMPDOC-1");
        assertThat(reloaded.getVersions()).hasSize(1);
        assertThat(reloaded.currentVersion().sha256()).isEqualTo("sha256-v1");
        assertThat(reloaded.getShares()).hasSize(1);
        assertThat(reloaded.getShares().iterator().next().granteeUserId()).isEqualTo(PRACTITIONER);
    }

    @Test
    @DisplayName("Les versions s'empilent sans ecraser les precedentes")
    void versionsAccumulateAcrossSaves() {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Ordonnance",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "medical-documents/42/v1.pdf",
                "v1.pdf", "application/pdf", 10, "sha256-v1", PATIENT, CLOCK);
        MedicalDocument saved = repository.save(document);

        saved.addVersion("medical-documents/42/v2.pdf", "v2.pdf", "application/pdf", 20,
                "sha256-v2", "Correction", PATIENT, CLOCK);
        MedicalDocument reloaded = repository.save(saved);

        assertThat(reloaded.getVersions()).hasSize(2);
        assertThat(reloaded.currentVersionNumber()).isEqualTo(2);
        assertThat(reloaded.getVersions().get(0).sha256()).isEqualTo("sha256-v1");
    }

    @Test
    @DisplayName("findSharedWith ne renvoie que les partages non revoques et non expires")
    void findSharedWithKeepsOnlyActiveShares() {
        MedicalDocument active = repository.save(MedicalDocument.upload(PATIENT, "Actif",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "k1", "a.pdf", "application/pdf",
                10, "s1", PATIENT, CLOCK));
        MedicalDocument revoked = MedicalDocument.upload(PATIENT, "Revoque",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "k2", "b.pdf", "application/pdf",
                10, "s2", PATIENT, CLOCK);
        MedicalDocument.DocumentShare shareToRevoke = revoked.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        revoked.revokeShare(shareToRevoke.id(), CLOCK);
        MedicalDocument expired = MedicalDocument.upload(PATIENT, "Expire",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "k3", "c.pdf", "application/pdf",
                10, "s3", PATIENT, CLOCK);
        expired.getShares().add(new MedicalDocument.DocumentShare("share-expired", PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Avis", Instant.now().minusSeconds(3_600),
                Instant.now().minusSeconds(60), null));
        repository.save(revoked);
        repository.save(expired);

        MedicalDocument shared = repository.findById(active.getId()).orElseThrow();
        shared.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD, "Avis",
                Instant.now().plusSeconds(3_600), Clock.systemUTC());
        repository.save(shared);

        List<MedicalDocument> forPractitioner = repository.findSharedWith(PRACTITIONER);

        assertThat(forPractitioner).hasSize(1);
        assertThat(forPractitioner.get(0).getId()).isEqualTo(active.getId());
    }

    @Test
    @DisplayName("La revocation conserve la trace (revokedAt) sans supprimer la ligne")
    void revokedShareKeepsTrace() {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Ordonnance",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "k", "a.pdf", "application/pdf",
                10, "s", PATIENT, CLOCK);
        MedicalDocument.DocumentShare share = document.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        MedicalDocument saved = repository.save(document);

        MedicalDocument updated = repository.findById(saved.getId()).orElseThrow();
        updated.revokeShare(share.id(), CLOCK);
        MedicalDocument reloaded = repository.save(updated);

        assertThat(reloaded.getShares()).hasSize(1);
        MedicalDocument.DocumentShare reloadedShare = reloaded.getShares().iterator().next();
        assertThat(reloadedShare.revokedAt()).isEqualTo(CLOCK.instant());
        assertThat(reloadedShare.isActive(CLOCK.instant())).isFalse();
    }

    @Test
    @DisplayName("findByOwnerUserId restitue les documents du patient")
    void findByOwnerReturnsOwnedDocuments() {
        repository.save(MedicalDocument.upload(PATIENT, "Un", null, "k1", "a.pdf",
                "application/pdf", 10, "s1", PATIENT, CLOCK));
        repository.save(MedicalDocument.upload(PATIENT, "Deux", null, "k2", "b.pdf",
                "application/pdf", 10, "s2", PATIENT, CLOCK));
        repository.save(MedicalDocument.upload(PRACTITIONER, "D'un autre", null, "k3", "c.pdf",
                "application/pdf", 10, "s3", PRACTITIONER, CLOCK));

        List<MedicalDocument> owned = repository.findByOwnerUserId(PATIENT);

        assertThat(owned).hasSize(2)
                .allMatch(document -> PATIENT.equals(document.getOwnerUserId()));
    }

    @Test
    @DisplayName("deleteByOwnerUserId purge documents et partages associes")
    void deleteByOwnerPurgesShares() {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Ordonnance",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "k", "a.pdf", "application/pdf",
                10, "s", PATIENT, CLOCK);
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        Long id = repository.save(document).getId();

        repository.deleteByOwnerUserId(PATIENT);

        assertThat(repository.findById(id)).isEqualTo(Optional.empty());
        assertThat(repository.findSharedWith(PRACTITIONER)).isEmpty();
    }
}
