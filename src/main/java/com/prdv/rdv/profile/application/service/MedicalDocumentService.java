package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.port.input.MedicalDocumentUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cas d'usage : televersement et cycle de vie des documents medicaux
 * (module 2.1 — Documents Médicaux).
 *
 * <p>Pipeline d'entree : controle du format -&gt; stockage -&gt; empreinte
 * SHA-256 -&gt; OCR -&gt; classification automatique -&gt; persistance.
 * Chaque etape technique est derriere un port (Strategy) : remplacer le
 * moteur OCR ou le classifieur par une IA n'a aucun impact ici
 * (Open/Closed, Dependency Inversion).
 */
@Service
public class MedicalDocumentService implements MedicalDocumentUseCase {

    private static final String DICOM_DEFAULT_MODALITY = "OT";

    private final MedicalDocumentRepository documentRepository;
    private final MedicalFileStoragePort fileStorage;
    private final OcrExtractionPort ocrExtraction;
    private final DocumentClassificationPort classification;
    private final MedicalFilePolicy filePolicy;
    private final CurrentUserPort currentUser;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileProperties properties;
    private final Clock clock;

    public MedicalDocumentService(MedicalDocumentRepository documentRepository,
                                  MedicalFileStoragePort fileStorage,
                                  OcrExtractionPort ocrExtraction,
                                  DocumentClassificationPort classification,
                                  MedicalFilePolicy filePolicy,
                                  CurrentUserPort currentUser,
                                  ProfileViewMapper viewMapper,
                                  ProfileAuditTrail auditTrail,
                                  ProfileEventPublisher eventPublisher,
                                  ProfileProperties properties,
                                  Clock clock) {
        this.documentRepository = documentRepository;
        this.fileStorage = fileStorage;
        this.ocrExtraction = ocrExtraction;
        this.classification = classification;
        this.filePolicy = filePolicy;
        this.currentUser = currentUser;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProfileViews.MedicalDocumentView upload(DocumentCommands.UploadDocument command) {
        Long userId = currentUser.requireCurrentUserId();
        filePolicy.validateDocument(command.contentType(), command.content(),
                properties.getDocuments().getMaxBytes());

        boolean dicom = filePolicy.isDicom(command.contentType(), command.content());
        String sha256 = filePolicy.sha256(command.content());
        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getDocuments().getFolder() + "/" + userId,
                command.originalFilename(), command.contentType(), command.content());

        MedicalDocument document = MedicalDocument.upload(userId, command.title(), command.category(),
                stored.storageKey(), command.originalFilename(), command.contentType(), stored.sizeBytes(),
                sha256, userId, clock);

        MedicalDocument.OcrResult ocr = ocrExtraction.extract(command.originalFilename(),
                command.contentType(), command.content());
        document.applyOcr(ocr, clock);

        if (dicom) {
            document.recordDicomMetadata(command.dicomModality() == null
                    ? DICOM_DEFAULT_MODALITY : command.dicomModality(),
                    command.dicomStudyDescription(), clock);
        }

        if (command.category() == null) {
            classifyAutomatically(document, command, ocr);
        }

        MedicalDocument saved = documentRepository.save(document);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED, userId,
                "MedicalDocument", String.valueOf(saved.getId()),
                (saved.getCategory() == null ? "non classe" : saved.getCategory().name())
                        + " / " + command.contentType() + " / " + stored.sizeBytes() + " octets"
                        + (dicom ? " (DICOM)" : ""));
        eventPublisher.publish(new ProfileEvent.MedicalDocumentUploaded(userId, saved.getId(),
                saved.getCategory() == null ? null : saved.getCategory().name(),
                saved.currentVersionNumber(), clock.instant()));
        return viewMapper.documentView(saved);
    }

    @Override
    @Transactional
    public ProfileViews.MedicalDocumentView addVersion(DocumentCommands.AddVersion command) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(command.documentId(), userId);
        filePolicy.validateDocument(command.contentType(), command.content(),
                properties.getDocuments().getMaxBytes());

        MedicalFileStoragePort.StoredFile stored = fileStorage.store(
                properties.getDocuments().getFolder() + "/" + userId,
                command.originalFilename(), command.contentType(), command.content());
        MedicalDocument.DocumentVersion version = document.addVersion(stored.storageKey(),
                command.originalFilename(), command.contentType(), stored.sizeBytes(),
                filePolicy.sha256(command.content()), command.changeNote(), userId, clock);

        MedicalDocument saved = documentRepository.save(document);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED, userId,
                "MedicalDocument", String.valueOf(saved.getId()), "nouvelle version v" + version.version());
        eventPublisher.publish(new ProfileEvent.MedicalDocumentUploaded(userId, saved.getId(),
                saved.getCategory() == null ? null : saved.getCategory().name(), version.version(),
                clock.instant()));
        return viewMapper.documentView(saved);
    }

    @Override
    @Transactional
    public ProfileViews.MedicalDocumentView reclassify(DocumentCommands.Reclassify command) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(command.documentId(), userId);
        document.applyClassification(new MedicalDocument.ClassificationResult(command.category(), 1.0,
                MedicalDocument.ClassificationSource.USER_DEFINED, "patient", clock.instant()), clock);
        MedicalDocument saved = documentRepository.save(document);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED, userId,
                "MedicalDocument", String.valueOf(saved.getId()),
                "requalification manuelle : " + command.category());
        return viewMapper.documentView(saved);
    }

    @Override
    @Transactional
    public void archive(Long documentId) {
        Long userId = currentUser.requireCurrentUserId();
        MedicalDocument document = requireOwnedDocument(documentId, userId);
        document.archive(clock);
        documentRepository.save(document);
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_DOCUMENT_UPLOADED, userId,
                "MedicalDocument", String.valueOf(documentId), "document archive");
    }

    // ------------------------------------------------------------------

    private void classifyAutomatically(MedicalDocument document, DocumentCommands.UploadDocument command,
                                       MedicalDocument.OcrResult ocr) {
        DocumentClassificationPort.ClassificationRequest request =
                new DocumentClassificationPort.ClassificationRequest(command.originalFilename(),
                        command.contentType(),
                        ocr == null ? null : ocr.rawText(),
                        document.getDicomModality(),
                        command.content());
        DocumentClassificationPort.Classification result = classification.classify(request);
        if (result == null || result.category() == null) {
            return;
        }
        document.applyClassification(new MedicalDocument.ClassificationResult(result.category(),
                result.confidence(), MedicalDocument.ClassificationSource.AUTOMATIC_AI,
                classification.name(), clock.instant()), clock);
        eventPublisher.publish(new ProfileEvent.MedicalDocumentClassified(document.getId(),
                result.category().name(), result.confidence(), classification.name(), clock.instant()));
    }

    private MedicalDocument requireOwnedDocument(Long documentId, Long userId) {
        MedicalDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.DOCUMENT_NOT_FOUND,
                        "Document " + documentId + " introuvable"));
        if (!document.getOwnerUserId().equals(userId)) {
            throw ProfileException.of(ProfileErrorCode.ACCESS_DENIED,
                    "Seul le proprietaire peut modifier ce document");
        }
        return document;
    }
}
