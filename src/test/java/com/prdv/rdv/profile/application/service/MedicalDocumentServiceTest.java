package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.DocumentClassificationPort;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.MedicalFileStoragePort;
import com.prdv.rdv.profile.application.port.output.OcrExtractionPort;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.config.ProfileProperties;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Map;
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
 * Tests unitaires du cas d'usage « televersement et cycle de vie des
 * documents » : pipeline d'entree (controle, stockage, empreinte, OCR,
 * classification), versioning, requalification et archivage.
 *
 * <p>Les regles fichier et le mapping des vues sont reels (leurs propres
 * tests les couvrent) ; les ports sortants sont des mocks (Mockito), ce qui
 * isole strictement le cas d'usage.
 */
@ExtendWith(MockitoExtension.class)
class MedicalDocumentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"),
            ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long OTHER_USER = 7L;
    private static final Long DOCUMENT_ID = 99L;

    @Mock
    private MedicalDocumentRepository documentRepository;
    @Mock
    private MedicalFileStoragePort fileStorage;
    @Mock
    private OcrExtractionPort ocrExtraction;
    @Mock
    private DocumentClassificationPort classification;
    @Mock
    private CurrentUserPort currentUser;
    @Mock
    private ProfileAuditPort auditPort;
    @Mock
    private ProfileEventPublisher eventPublisher;

    private MedicalDocumentService service;

    @BeforeEach
    void setUp() {
        ProfileProperties properties = new ProfileProperties();
        service = new MedicalDocumentService(documentRepository, fileStorage, ocrExtraction,
                classification, new MedicalFilePolicy(), currentUser,
                new ProfileViewMapper(CLOCK), new ProfileAuditTrail(auditPort), eventPublisher,
                properties, CLOCK);
    }

    // ------------------------------------------------------------------
    // Televersement
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Le televersement stocke le fichier, empreinte le contenu et trace l'audit")
    void uploadStoresHashesAndAudits() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        byte[] content = "ordonnance".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.store(anyString(), anyString(), anyString(), any(byte[].class)))
                .thenReturn(new MedicalFileStoragePort.StoredFile("medical-documents/42/f.pdf",
                        content.length));
        when(ocrExtraction.extract(anyString(), anyString(), any(byte[].class)))
                .thenReturn(MedicalDocument.OcrResult.unsupported("text-extractor-v1",
                        CLOCK.instant()));
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument saved = invocation.getArgument(0);
            saved.setId(DOCUMENT_ID);
            return saved;
        });

        ProfileViews.MedicalDocumentView view = service.upload(new DocumentCommands.UploadDocument(
                "Ordonnance du 28/09", MedicalDocument.DocumentCategory.PRESCRIPTION,
                "ordonnance.pdf", "application/pdf", content, null, null));

        assertThat(view.id()).isEqualTo(DOCUMENT_ID);
        assertThat(view.category()).isEqualTo(MedicalDocument.DocumentCategory.PRESCRIPTION);
        assertThat(view.status()).isEqualTo(MedicalDocument.DocumentStatus.CLASSIFIED);
        assertThat(view.classificationSource())
                .isEqualTo(MedicalDocument.ClassificationSource.USER_DEFINED);
        assertThat(view.currentVersion()).isEqualTo(1);
        assertThat(view.versions()).singleElement()
                .extracting(ProfileViews.DocumentVersionView::sha256).asString().hasSize(64);

        verify(fileStorage).store(eq("medical-documents/" + PATIENT), eq("ordonnance.pdf"),
                eq("application/pdf"), eq(content));
        verify(classification, never()).classify(any());
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED),
                eq(PATIENT), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(true));
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.isA(
                ProfileEvent.MedicalDocumentUploaded.class));
    }

    @Test
    @DisplayName("Sans categorie demandee, l'OCR alimente la classification automatique")
    void uploadWithoutCategoryTriggersOcrAndClassification() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        byte[] content = "resultats biologie".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.store(anyString(), anyString(), anyString(), any(byte[].class)))
                .thenReturn(new MedicalFileStoragePort.StoredFile("medical-documents/42/a.pdf",
                        content.length));
        MedicalDocument.OcrResult ocr = new MedicalDocument.OcrResult(
                MedicalDocument.OcrStatus.SUCCEEDED, "glycemie 1,08", Map.of("lab_glycemie", "1,08"),
                0.75, "text-extractor-v1", CLOCK.instant());
        when(ocrExtraction.extract(anyString(), anyString(), any(byte[].class))).thenReturn(ocr);
        when(classification.classify(any())).thenReturn(new DocumentClassificationPort
                .Classification(MedicalDocument.DocumentCategory.LAB_RESULT, 0.9));
        when(classification.name()).thenReturn("keyword-lexicon-v1");
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument saved = invocation.getArgument(0);
            saved.setId(DOCUMENT_ID);
            return saved;
        });

        ProfileViews.MedicalDocumentView view = service.upload(new DocumentCommands.UploadDocument(
                "Analyses", null, "analyses.txt", "text/plain", content, null, null));

        assertThat(view.category()).isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
        assertThat(view.status()).isEqualTo(MedicalDocument.DocumentStatus.CLASSIFIED);
        assertThat(view.classificationSource())
                .isEqualTo(MedicalDocument.ClassificationSource.AUTOMATIC_AI);
        assertThat(view.ocr().status()).isEqualTo(MedicalDocument.OcrStatus.SUCCEEDED);

        ArgumentCaptor<ProfileEvent> events = ArgumentCaptor.forClass(ProfileEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publish(events.capture());
        assertThat(events.getAllValues())
                .hasExactlyElementsOfTypes(ProfileEvent.MedicalDocumentClassified.class,
                        ProfileEvent.MedicalDocumentUploaded.class);
        // L'evenement de classification porte l'identifiant reel (publie apres le save)
        ProfileEvent.MedicalDocumentClassified classified =
                (ProfileEvent.MedicalDocumentClassified) events.getAllValues().get(0);
        assertThat(classified.documentId()).isEqualTo(DOCUMENT_ID);
        assertThat(classified.category()).isEqualTo("LAB_RESULT");
    }

    @Test
    @DisplayName("Une confiance IA faible laisse le document a confirmer")
    void lowConfidenceClassificationStaysPending() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.store(anyString(), anyString(), anyString(), any(byte[].class)))
                .thenReturn(new MedicalFileStoragePort.StoredFile("k", content.length));
        when(ocrExtraction.extract(anyString(), anyString(), any(byte[].class)))
                .thenReturn(MedicalDocument.OcrResult.unsupported("text-extractor-v1",
                        CLOCK.instant()));
        when(classification.classify(any())).thenReturn(new DocumentClassificationPort
                .Classification(MedicalDocument.DocumentCategory.OTHER, 0.2));
        when(classification.name()).thenReturn("keyword-lexicon-v1");
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument saved = invocation.getArgument(0);
            saved.setId(DOCUMENT_ID);
            return saved;
        });

        ProfileViews.MedicalDocumentView view = service.upload(new DocumentCommands.UploadDocument(
                "Inconnu", null, "scan.bin", "application/octet-stream", content, null, null));

        assertThat(view.status()).isEqualTo(MedicalDocument.DocumentStatus.PENDING_CLASSIFICATION);
        assertThat(view.category()).isEqualTo(MedicalDocument.DocumentCategory.OTHER);
    }

    @Test
    @DisplayName("Un fichier DICOM enregistre ses metadonnees (modalite par defaut OT)")
    void dicomMetadataIsRecorded() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        byte[] dicom = new byte[132];
        Arrays.fill(dicom, (byte) 0);
        dicom[128] = 'D';
        dicom[129] = 'I';
        dicom[130] = 'C';
        dicom[131] = 'M';
        when(fileStorage.store(anyString(), anyString(), anyString(), any(byte[].class)))
                .thenReturn(new MedicalFileStoragePort.StoredFile("k", dicom.length));
        when(ocrExtraction.extract(anyString(), anyString(), any(byte[].class)))
                .thenReturn(MedicalDocument.OcrResult.unsupported("text-extractor-v1",
                        CLOCK.instant()));
        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument saved = invocation.getArgument(0);
            saved.setId(DOCUMENT_ID);
            return saved;
        });

        ProfileViews.MedicalDocumentView view = service.upload(new DocumentCommands.UploadDocument(
                "IRM", MedicalDocument.DocumentCategory.IMAGING_MRI, "serie.dcm",
                "application/dicom", dicom, null, "Etude genou"));

        assertThat(view.dicomModality()).isEqualTo("OT");
        assertThat(view.dicomStudyDescription()).isEqualTo("Etude genou");
    }

    @Test
    @DisplayName("Un format non medical est refuse avant tout stockage")
    void rejectsUnsupportedFormatBeforeStorage() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);

        assertThatThrownBy(() -> service.upload(new DocumentCommands.UploadDocument(
                "Malveillant", null, "virus.exe", "application/x-msdownload",
                new byte[]{1, 2}, null, null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_FORMAT_UNSUPPORTED);

        verify(fileStorage, never()).store(anyString(), anyString(), anyString(), any(byte[].class));
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un document trop volumineux est refuse avec DOCUMENT_TOO_LARGE")
    void rejectsTooLargeDocument() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        byte[] huge = new byte[26 * 1024 * 1024];

        assertThatThrownBy(() -> service.upload(new DocumentCommands.UploadDocument(
                "Trop gros", null, "gros.pdf", "application/pdf", huge, null, null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_TOO_LARGE);
    }

    // ------------------------------------------------------------------
    // Versioning
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Une nouvelle version s'empile et publie l'evenement de version")
    void addVersionKeepsHistory() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(ownedDocument()));
        byte[] content = "v2".getBytes(StandardCharsets.UTF_8);
        when(fileStorage.store(anyString(), anyString(), anyString(), any(byte[].class)))
                .thenReturn(new MedicalFileStoragePort.StoredFile("k2", content.length));
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileViews.MedicalDocumentView view = service.addVersion(
                new DocumentCommands.AddVersion(DOCUMENT_ID, "v2.pdf", "application/pdf", content,
                        "Correction du dosage"));

        assertThat(view.currentVersion()).isEqualTo(2);
        assertThat(view.versions()).hasSize(2);
        assertThat(view.versions().get(1).changeNote()).isEqualTo("Correction du dosage");
    }

    @Test
    @DisplayName("Seul le proprietaire peut ajouter une version")
    void addVersionDeniedForNonOwner() {
        when(currentUser.requireCurrentUserId()).thenReturn(OTHER_USER);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(ownedDocument()));

        assertThatThrownBy(() -> service.addVersion(new DocumentCommands.AddVersion(DOCUMENT_ID,
                "v2.pdf", "application/pdf", new byte[]{1}, null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("Un document archive est immutable")
    void addVersionOnArchivedDocumentIsRejected() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        MedicalDocument document = ownedDocument();
        document.archive(CLOCK);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.addVersion(new DocumentCommands.AddVersion(DOCUMENT_ID,
                "v2.pdf", "application/pdf", new byte[]{1}, null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_IMMUTABLE);
    }

    // ------------------------------------------------------------------
    // Requalification et archivage
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La requalification manuelle est tracee comme USER_DEFINED")
    void reclassifyIsUserDefined() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(ownedDocument()));
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileViews.MedicalDocumentView view = service.reclassify(new DocumentCommands.Reclassify(
                DOCUMENT_ID, MedicalDocument.DocumentCategory.LAB_RESULT));

        assertThat(view.category()).isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
        assertThat(view.classificationSource())
                .isEqualTo(MedicalDocument.ClassificationSource.USER_DEFINED);
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED),
                eq(PATIENT), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(true));
    }

    @Test
    @DisplayName("L'archivage passe le document en ARCHIVED")
    void archiveMarksDocumentArchived() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(ownedDocument()));
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.archive(DOCUMENT_ID);

        ArgumentCaptor<MedicalDocument> saved = ArgumentCaptor.forClass(MedicalDocument.class);
        verify(documentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(MedicalDocument.DocumentStatus.ARCHIVED);
        assertThat(saved.getValue().getArchivedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("Un document inconnu est signale DOCUMENT_NOT_FOUND")
    void missingDocumentIsSignalled() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(DOCUMENT_ID))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.DOCUMENT_NOT_FOUND);
    }

    // ------------------------------------------------------------------

    private static MedicalDocument ownedDocument() {
        return MedicalDocument.upload(PATIENT, "Ordonnance", MedicalDocument.DocumentCategory.PRESCRIPTION,
                "medical-documents/42/f.pdf", "ordonnance.pdf", "application/pdf", 10,
                "sha256-v1", PATIENT, CLOCK);
    }
}
