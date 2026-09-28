package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.DmpGatewayPort;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires du cas d'usage « partage securise et DMP » : permissions
 * bornees, revocation tracée, et exigence de consentement avant publication
 * au Dossier Medical Partage national.
 */
@ExtendWith(MockitoExtension.class)
class DocumentSharingServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-28T10:00:00Z"),
            ZoneOffset.UTC);
    private static final Long PATIENT = 42L;
    private static final Long PRACTITIONER = 7L;
    private static final Long DOCUMENT_ID = 99L;

    @Mock
    private MedicalDocumentRepository documentRepository;
    @Mock
    private PatientIdentityRepository identityRepository;
    @Mock
    private DmpGatewayPort dmpGateway;
    @Mock
    private CurrentUserPort currentUser;
    @Mock
    private ProfileAccessGuard accessGuard;
    @Mock
    private ProfileAuditPort auditPort;
    @Mock
    private ProfileEventPublisher eventPublisher;

    private DocumentSharingService service;

    @BeforeEach
    void setUp() {
        service = new DocumentSharingService(documentRepository, identityRepository, dmpGateway,
                currentUser, accessGuard, new ProfileViewMapper(CLOCK),
                new ProfileAuditTrail(auditPort), eventPublisher, CLOCK);
    }

    // ------------------------------------------------------------------
    // Partage entre utilisateurs
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Le partage ouvre l'acces au praticien, trace l'audit et publie l'evenement")
    void shareGrantsAccessAndPublishesEvent() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument()));
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileViews.DocumentShareView view = service.share(new DocumentCommands.ShareDocument(
                DOCUMENT_ID, PRACTITIONER, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD,
                "Avis cardiologie", CLOCK.instant().plusSeconds(3_600)));

        assertThat(view.granteeUserId()).isEqualTo(PRACTITIONER);
        assertThat(view.active()).isTrue();
        assertThat(view.id()).isNotBlank();
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_SHARED),
                eq(PATIENT), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(true));
        verify(eventPublisher).publish(isA(ProfileEvent.MedicalDocumentShared.class));
    }

    @Test
    @DisplayName("Un partage actif duplique est refuse")
    void duplicateActiveShareIsRejected() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        MedicalDocument document = ownedDocument();
        document.shareWith(PRACTITIONER, MedicalDocument.SharePermission.VIEW, "Deja partage", null,
                CLOCK);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.share(new DocumentCommands.ShareDocument(DOCUMENT_ID,
                PRACTITIONER, MedicalDocument.SharePermission.VIEW, "Encore", null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("La revocation conserve la trace du partage")
    void revokeShareKeepsTrace() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        MedicalDocument document = ownedDocument();
        MedicalDocument.DocumentShare share = document.shareWith(PRACTITIONER,
                MedicalDocument.SharePermission.VIEW, "Avis", null, CLOCK);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileViews.DocumentShareView view = service.revokeShare(DOCUMENT_ID, share.id());

        assertThat(view.active()).isFalse();
        assertThat(view.revokedAt()).isEqualTo(CLOCK.instant());
    }

    @Test
    @DisplayName("Un tiers ne peut pas enumerer les partages d'un document")
    void strangerCannotListShares() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        MedicalDocument document = ownedDocument();
        document.getShares().add(new MedicalDocument.DocumentShare("share-x", 13L,
                MedicalDocument.SharePermission.VIEW, "Tiers", CLOCK.instant(), null, null));
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.sharesOf(DOCUMENT_ID))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);
    }

    // ------------------------------------------------------------------
    // Dossier Medical Partage national
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sans consentement DMP, la publication est refusee (CONSENT_REQUIRED)")
    void pushToDmpRequiresConsent() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument()));
        when(accessGuard.preferencesOf(PATIENT))
                .thenReturn(PrivacyPreferences.defaults(PATIENT, CLOCK));

        assertThatThrownBy(() -> service.pushToDmp(
                new DocumentCommands.PushToDmp(DOCUMENT_ID)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.CONSENT_REQUIRED);

        verify(dmpGateway, never()).publishDocument(any(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Sans compte DMP rattache, la publication est refusee")
    void pushToDmpRequiresLinkedAccount() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument()));
        when(accessGuard.preferencesOf(PATIENT)).thenReturn(preferencesWithDmpConsent());
        when(identityRepository.findByUserId(PATIENT))
                .thenReturn(Optional.of(PatientIdentity.create(PATIENT, null, CLOCK)));

        assertThatThrownBy(() -> service.pushToDmp(
                new DocumentCommands.PushToDmp(DOCUMENT_ID)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Avec consentement et compte rattache, le document part dans le DMP")
    void pushToDmpPublishesAndMarksDocument() {
        when(currentUser.requireCurrentUserId()).thenReturn(PATIENT);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument()));
        when(accessGuard.preferencesOf(PATIENT)).thenReturn(preferencesWithDmpConsent());
        PatientIdentity identity = PatientIdentity.create(PATIENT, null, CLOCK);
        identity.linkDmp("DMP-42-ABC", true, CLOCK);
        when(identityRepository.findByUserId(PATIENT)).thenReturn(Optional.of(identity));
        when(dmpGateway.publishDocument(eq(PATIENT), eq("DMP-42-ABC"), anyString(), anyString()))
                .thenReturn("DMPDOC-123");
        when(documentRepository.save(any(MedicalDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileViews.MedicalDocumentView view = service.pushToDmp(
                new DocumentCommands.PushToDmp(DOCUMENT_ID));

        assertThat(view.dmpReference()).isEqualTo("DMPDOC-123");
        assertThat(view.dmpSharedAt()).isEqualTo(CLOCK.instant());
        verify(auditPort).record(eq(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_SHARED),
                eq(PATIENT), eq("MedicalDocument"), eq(String.valueOf(DOCUMENT_ID)), anyString(),
                eq(true));
    }

    @Test
    @DisplayName("Seul le proprietaire peut partager son document")
    void onlyOwnerCanShare() {
        when(currentUser.requireCurrentUserId()).thenReturn(PRACTITIONER);
        when(documentRepository.findById(DOCUMENT_ID))
                .thenReturn(Optional.of(ownedDocument()));

        assertThatThrownBy(() -> service.share(new DocumentCommands.ShareDocument(DOCUMENT_ID,
                13L, MedicalDocument.SharePermission.VIEW, "Indu", null)))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.ACCESS_DENIED);
    }

    // ------------------------------------------------------------------

    private static MedicalDocument ownedDocument() {
        MedicalDocument document = MedicalDocument.upload(PATIENT, "Ordonnance",
                MedicalDocument.DocumentCategory.PRESCRIPTION, "medical-documents/42/f.pdf",
                "ordonnance.pdf", "application/pdf", 10, "sha256-v1", PATIENT, CLOCK);
        document.setId(DOCUMENT_ID);
        return document;
    }

    private static PrivacyPreferences preferencesWithDmpConsent() {
        PrivacyPreferences preferences = PrivacyPreferences.defaults(PATIENT, CLOCK);
        preferences.recordConsent(PrivacyPreferences.ConsentPurpose.DMP_SHARING, true, "2026-09",
                "127.0.0.1", CLOCK);
        return preferences;
    }
}
