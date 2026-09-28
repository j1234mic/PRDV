package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du cas d'usage « consultation et telechargement » : filtres,
 * granularite d'acces (« qui peut voir quoi ») et tracabilite des lectures.
 */
@ExtendWith(MockitoExtension.class)
class MedicalDocumentQueryServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"),
            ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long PRACTITIONER = 7L;
    private static final Long STRANGER = 13L;
    private static final Long DOCUMENT_ID = 99L;

    @Mock
    private MedicalDocumentRepository documentRepository;
    @Mock
    private MedicalFileStoragePort fileStorage;
    @Mock
    private CurrentUserPort currentUser;
    @Mock
    private ProfileAuditPort auditPort;

    private MedicalDocumentQueryService service;

    @BeforeEach
    void setUp() {
        service = new MedicalDocumentQueryService(documentRepository, fileStorage, currentUser,
                new ProfileViewMapper(CLOCK), new ProfileAuditTrail(auditPort), CLOCK);
    }

    @Test
    @DisplayName("Mes documents : filtre par categorie quand elle est demandee")
    void myDocumentsCanBeFilteredByCategory() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        MedicalDocument prescription = ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION);
        MedicalDocument lab = ownedDocument(MedicalDocument.DocumentCategory.LAB_RESULT);
        when(documentRepository.findByOwnerUserId(PATIENT)).thenReturn(List.of(prescription, lab));

        List<ProfileViews.MedicalDocumentView> all = service.myDocuments(null);
        List<ProfileViews.MedicalDocumentView> filtered =
                service.myDocuments(MedicalDocument.DocumentCategory.LAB_RESULT);

        assertThat(all).hasSize(2);
        assertThat(filtered).singleElement()
                .extracting(ProfileViews.MedicalDocumentView::category)
                .isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
    }

    @Test
    @DisplayName("Documents partages avec moi : seuls les partages actifs ouvrent l'acces")
    void sharedWithMeKeepsOnlyActiveShares() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        MedicalDocument activeShare = ownedDocument(100L,
                MedicalDocument.DocumentCategory.PRESCRIPTION);
        activeShare.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD,
                "Avis", null, CLOCK);
        MedicalDocument revokedShare = ownedDocument(101L,
                MedicalDocument.DocumentCategory.LAB_RESULT);
        MedicalDocument.DocumentShare revoked = revokedShare.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        revokedShare.revokeShare(revoked.id(), CLOCK);
        when(documentRepository.findSharedWith(PRACTITIONER))
                .thenReturn(List.of(activeShare, revokedShare));

        List<ProfileViews.MedicalDocumentView> shared = service.sharedWithMe();

        assertThat(shared).singleElement()
                .extracting(ProfileViews.MedicalDocumentView::id)
                .isEqualTo(100L);
    }

    @Test
    @DisplayName("La lecture d'un document non partage est refusee et tracee")
    void readingUnsharedDocumentIsDeniedAndAudited() {
        when(currentUser.requireCurrentUserId()).thenReturn(STRANGER);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION)));

        assertThatThrownBy(() -> service.document(DOCUMENT_ID))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);

        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED),
                eq(STRANGER), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(false));
    }

    @Test
    @DisplayName("Le proprietaire telecharge ses octets et l'acces est trace")
    void ownerCanDownloadBytes() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        MedicalDocument document = ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        byte[] content = "pdf".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.retrieve(document.currentVersion().storageKey())).thenReturn(content);

        ProfileViews.DocumentFile file = service.download(DOCUMENT_ID);

        assertThat(file.filename()).isEqualTo("ordonnance.pdf");
        assertThat(file.contentType()).isEqualTo("application/pdf");
        assertThat(file.sizeBytes()).isEqualTo(content.length);
        assertThat(file.content()).isEqualTo(content);
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED),
                eq(PATIENT), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(true));
    }

    @Test
    @DisplayName("Un partage VIEW ouvre la metadonnee, pas le telechargement des octets")
    void viewOnlyShareCannotDownload() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        MedicalDocument document = ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION);
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.download(DOCUMENT_ID))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);

        verify(fileStorage, never()).retrieve(anyString());
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED),
                eq(PRACTITIONER), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(false));
    }

    @Test
    @DisplayName("Un partage VIEW_AND_DOWNLOAD permet le telechargement")
    void downloadShareCanDownload() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        MedicalDocument document = ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION);
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD,
                "Avis", null, CLOCK);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        byte[] content = "pdf".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.retrieve(document.currentVersion().storageKey())).thenReturn(content);

        ProfileViews.DocumentFile file = service.download(DOCUMENT_ID);

        assertThat(file.content()).isEqualTo(content);
    }

    @Test
    @DisplayName("Un partage expire n'ouvre plus aucun acces")
    void expiredShareGrantsNothing() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        MedicalDocument document = ownedDocument(MedicalDocument.DocumentCategory.PRESCRIPTION);
        // Partage passe mais non revoque : il ne doit plus rien autoriser.
        document.getShares().add(new MedicalDocument.DocumentShare("share-expired", PRACTITIONER,
                MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD, "Avis",
                CLOCK.instant().minusSeconds(3_600), CLOCK.instant().minusSeconds(60), null));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.document(DOCUMENT_ID))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);
    }

    // ------------------------------------------------------------------

    private static MedicalDocument ownedDocument(MedicalDocument.DocumentCategory category) {
        return ownedDocument(DOCUMENT_ID, category);
    }

    private static MedicalDocument ownedDocument(Long id, MedicalDocument.DocumentCategory category) {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Ordonnance", category,
                "medical-documents/42/f.pdf", "ordonnance.pdf", "application/pdf", 10,
                "sha256-v1", PATIENT, CLOCK);
        document.setId(id);
        return document;
    }
}
