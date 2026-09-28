package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.port.input.MedicalDocumentQueryUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Cas d'usage : consultation et telechargement des documents medicaux.
 *
 * <p>Le controle d'acces precede toujours la lecture des octets : un
 * praticien ne recoit le fichier que si un partage actif le lui permet
 * (granularite « qui peut voir quoi »), et chaque telechargement est trace.
 */
@Service
public class MedicalDocumentQueryService implements MedicalDocumentQueryUseCase {

    private final MedicalDocumentRepository documentRepository;
    private final MedicalFileStoragePort fileStorage;
    private final CurrentUserPort currentUser;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final Clock clock;

    public MedicalDocumentQueryService(MedicalDocumentRepository documentRepository,
                                       MedicalFileStoragePort fileStorage,
                                       CurrentUserPort currentUser,
                                       ProfileViewMapper viewMapper,
                                       ProfileAuditTrail auditTrail,
                                       Clock clock) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.currentUser = currentUser;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.MedicalDocumentView> myDocuments(MedicalDocument.DocumentCategory category) {
        Long userId = currentUser.requireCurrentUserId();
        return documentRepository.findByOwnerUserId(userId).stream()
                .filter(document -> category == null || document.getCategory() == category)
                .map(viewMapper::documentView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.MedicalDocumentView> sharedWithMe() {
        Long userId = currentUser.requireCurrentUserId();
        return documentRepository.findSharedWith(userId).stream()
                .filter(document -> document.isAccessibleBy(userId, clock))
                .map(viewMapper::documentView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.MedicalDocumentView document(Long documentId) {
        return viewMapper.documentView(requireAccessibleDocument(documentId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DocumentFile download(Long documentId) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireAccessibleDocument(documentId);

        // Un partage VIEW ouvre la consultation des metadonnees, pas l'acces
        // aux octets : seuls le proprietaire et les partages VIEW_AND_DOWNLOAD
        // peuvent telecharger (granularite « qui peut voir quoi »).
        if (!document.allowsDownload(userId, clock)) {
            auditTrail.failure(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED, userId,
                    "MedicalDocument", String.valueOf(documentId),
                    "telechargement refuse (partage sans droit de telechargement)");
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Ce partage ne permet pas le telechargement du document");
        }

        MedicalDocument.DocumentVersion version = document.currentVersion();
        byte[] content = fileStorage.retrieve(version.storageKey());

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED, userId,
                "MedicalDocument", String.valueOf(documentId),
                "telechargement v" + version.version() + " (" + content.length + " octets)");
        return new ProfileViews.DocumentFile(version.originalFilename(), version.contentType(),
                content.length, content);
    }

    private MedicalDocument requireAccessibleDocument(Long documentId) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Document " + documentId + " introuvable"));
        if (!document.isAccessibleBy(userId, clock)) {
            auditTrail.failure(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_DOWNLOADED, userId,
                    "MedicalDocument", String.valueOf(documentId), "acces refuse (aucun partage actif)");
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Ce document ne vous est pas partage");
        }
        return document;
    }
}
