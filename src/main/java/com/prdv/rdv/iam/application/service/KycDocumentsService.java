package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.port.input.KycDocumentsUseCase;
import com.prdv.rdv.iam.application.port.output.DocumentStoragePort;
import com.prdv.rdv.iam.application.port.output.IdentityVerificationPort;
import com.prdv.rdv.iam.application.port.output.KycDocumentRepository;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Depot et traitement des pieces justatives KYC.
 * Le stockage du fichier et la verification d'identite sont des ports
 * (stockage local/S3, prestataire KYC) : le service orchestre sans dependre
 * d'un fournisseur (Dependency Inversion).
 */
@Service
public class KycDocumentsService implements KycDocumentsUseCase {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final String NS_IDENTITY = "IDENTITY_DOC";

    private final KycDocumentRepository documentRepository;
    private final DocumentStoragePort documentStorage;
    private final IdentityVerificationPort identityVerification;
    private final PatientProfileRepository patientProfileRepository;
    private final TokenizationPort tokenization;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public KycDocumentsService(KycDocumentRepository documentRepository,
                               DocumentStoragePort documentStorage,
                               IdentityVerificationPort identityVerification,
                               PatientProfileRepository patientProfileRepository,
                               TokenizationPort tokenization,
                               ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.documentRepository = documentRepository;
        this.documentStorage = documentStorage;
        this.identityVerification = identityVerification;
        this.patientProfileRepository = patientProfileRepository;
        this.tokenization = tokenization;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.KycDocumentView upload(ProfileCommands.UploadKycDocument command) {
        if (command.content() == null || command.content().length == 0) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Fichier vide");
        }
        if (command.content().length > MAX_FILE_BYTES) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Fichier trop volumineux (max 10 Mo)");
        }

        DocumentStoragePort.StoredDocument stored = documentStorage.store(
                "user-" + command.ownerUserId(), command.originalFilename(),
                command.contentType(), command.content());

        KycDocument document = KycDocument.upload(command.ownerUserId(), command.type(),
                stored.storageKey(), command.originalFilename(), command.contentType(),
                stored.sizeBytes(), clock);

        // Verification d'identite automatisee pour les pieces des patients (CNI / passeport)
        if (command.type() == KycDocument.DocumentType.IDENTITY_CARD
                || command.type() == KycDocument.DocumentType.PASSPORT) {
            IdentityVerificationPort.IdentityCheck check = identityVerification.verify(document);
            if (check.verified()) {
                document.review(KycDocument.ReviewStatus.VERIFIED,
                        "Verification automatique : " + check.provider() + " (" + check.reference() + ")", clock);
                upgradePatientKyc(command.ownerUserId(), stored.storageKey());
            }
        }

        KycDocument saved = documentRepository.save(document);
        auditLogger.record(command.ownerUserId(), AuditLog.Action.KYC_DOCUMENT_UPLOADED,
                AuditLog.Outcome.SUCCESS, "KycDocument", String.valueOf(saved.getId()),
                command.type().name(), null);
        return viewMapper.kycView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.KycDocumentView> listForOwner(Long ownerUserId) {
        return documentRepository.findByOwnerUserId(ownerUserId).stream()
                .map(viewMapper::kycView).toList();
    }

    private void upgradePatientKyc(Long ownerUserId, String storageKey) {
        patientProfileRepository.findByUserId(ownerUserId).ifPresent(patient -> {
            patient.setIdentityDocumentToken(tokenization.tokenize(storageKey, NS_IDENTITY));
            patient.upgradeKyc(PatientProfile.KycLevel.VERIFIED, clock);
            patientProfileRepository.save(patient);
        });
    }
}
