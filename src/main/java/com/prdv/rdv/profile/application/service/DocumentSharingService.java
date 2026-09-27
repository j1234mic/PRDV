package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.port.input.DocumentSharingUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Cas d'usage : partage securise de documents (praticiens nommement designes,
 * avec permission et expiration) et transmission au DMP national.
 */
@Service
public class DocumentSharingService implements DocumentSharingUseCase {

    private final MedicalDocumentRepository documentRepository;
    private final PatientIdentityRepository identityRepository;
    private final DmpGatewayPort dmpGateway;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final Clock clock;

    public DocumentSharingService(MedicalDocumentRepository documentRepository,
                                  PatientIdentityRepository identityRepository,
                                  DmpGatewayPort dmpGateway,
                                  CurrentUserPort currentUser,
                                  ProfileAccessGuard accessGuard,
                                  ProfileViewMapper viewMapper,
                                  ProfileAuditTrail auditTrail,
                                  ProfileEventPublisher eventPublisher,
                                  Clock clock) {
        this.documentRepository = documentRepository;
        this.identityRepository = identityRepository;
        this.dmpGateway = dmpGateway;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProfileViews.DocumentShareView share(DocumentCommands.ShareDocument command) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(command.documentId(), userId);
        MedicalDocument.DocumentShare share = document.shareWith(command.granteeUserId(),
                command.permission(), command.reason(), command.expiresAt(), clock);
        documentRepository.save(document);

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_SHARED, userId,
                "MedicalDocument", String.valueOf(document.getId()),
                "partage avec " + share.granteeUserId() + " (" + share.permission() + ")");
        eventPublisher.publish(new ProfileEvent.MedicalDocumentShared(userId, document.getId(),
                share.granteeUserId(), share.permission().name(), clock.instant()));
        return viewMapper.shareView(share, clock.instant());
    }

    @Override
    @Transactional
    public ProfileViews.DocumentShareView revokeShare(Long documentId, String shareId) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(documentId, userId);
        MedicalDocument.DocumentShare revoked = document.revokeShare(shareId, clock);
        documentRepository.save(document);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_SHARED, userId,
                "MedicalDocument", String.valueOf(documentId), "partage revoque " + shareId);
        return viewMapper.shareView(revoked, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.DocumentShareView> sharesOf(Long documentId) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Document " + documentId + " introuvable"));
        if (!document.isAccessibleBy(userId, clock)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Ce document ne vous est pas partage");
        }
        return document.getShares().stream()
                .map(share -> viewMapper.shareView(share, clock.instant()))
                .toList();
    }

    @Override
    @Transactional
    public ProfileViews.MedicalDocumentView pushToDmp(DocumentCommands.PushToDmp command) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(command.documentId(), userId);

        PrivacyPreferences preferences = accessGuard.preferencesOf(userId);
        if (!preferences.hasConsent(PrivacyPreferences.ConsentPurpose.DMP_SHARING)) {
            throw ProfileException.of(ProfileErrorCode.CONSENT_REQUIRED,
                    "Le partage avec le DMP national n'est pas autorise (consentement manquant)");
        }
        PatientIdentity identity = identityRepository.findByUserId(userId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.PROFILE_NOT_FOUND,
                        "Profil patient introuvable"));
        PatientIdentity.DmpAccount dmp = identity.getDmpAccount();
        if (dmp == null || !dmp.linked()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Aucun DMP rattache : publication impossible");
        }

        String reference = dmpGateway.publishDocument(userId, dmp.dmpIdentifier(), document.getTitle(),
                document.currentVersion().contentType());
        document.markSharedWithDmp(reference, clock);
        MedicalDocument saved = documentRepository.save(document);

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_SHARED, userId,
                "MedicalDocument", String.valueOf(saved.getId()), "publie dans le DMP : " + reference);
        return viewMapper.documentView(saved);
    }

    private MedicalDocument requireOwnedDocument(Long documentId, Long userId) {
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Document " + documentId + " introuvable"));
        if (!document.getOwnerUserId().equals(userId)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Seul le proprietaire peut partager ce document");
        }
        return document;
    }
}
