package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.ApplicationReviewUseCase;
import com.prdv.rdv.iam.application.port.output.ContractRepository;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.application.port.output.KycDocumentRepository;
import com.prdv.rdv.iam.application.port.output.NotificationPort;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.PractitionerProfileRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation manuelle par les moderateurs : dossiers praticiens / etablissements
 * et revue des documents KYC en attente.
 */
@Service
public class ApplicationReviewService implements ApplicationReviewUseCase {

    private final UserRepository userRepository;
    private final PractitionerProfileRepository practitionerRepository;
    private final EstablishmentProfileRepository establishmentRepository;
    private final PatientProfileRepository patientRepository;
    private final KycDocumentRepository documentRepository;
    private final ContractRepository contractRepository;
    private final NotificationPort notificationPort;
    private final DomainEventPublisher eventPublisher;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public ApplicationReviewService(UserRepository userRepository,
                                    PractitionerProfileRepository practitionerRepository,
                                    EstablishmentProfileRepository establishmentRepository,
                                    PatientProfileRepository patientRepository,
                                    KycDocumentRepository documentRepository,
                                    ContractRepository contractRepository,
                                    NotificationPort notificationPort,
                                    DomainEventPublisher eventPublisher,
                                    ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.practitionerRepository = practitionerRepository;
        this.establishmentRepository = establishmentRepository;
        this.patientRepository = patientRepository;
        this.documentRepository = documentRepository;
        this.contractRepository = contractRepository;
        this.notificationPort = notificationPort;
        this.eventPublisher = eventPublisher;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.PractitionerView> pendingPractitioners() {
        List<Views.PractitionerView> result = new ArrayList<>();
        for (PractitionerProfile profile : practitionerRepository.findPendingValidation()) {
            userRepository.findById(profile.getUserId())
                    .filter(u -> u.getStatus() == AccountStatus.PENDING_VALIDATION)
                    .ifPresent(u -> result.add(viewMapper.practitionerView(u, profile)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.EstablishmentView> pendingEstablishments() {
        List<Views.EstablishmentView> result = new ArrayList<>();
        for (EstablishmentProfile profile : establishmentRepository.findPendingValidation()) {
            userRepository.findById(profile.getUserId())
                    .filter(u -> u.getStatus() == AccountStatus.PENDING_VALIDATION)
                    .ifPresent(u -> result.add(viewMapper.establishmentView(u, profile)));
        }
        return result;
    }

    @Override
    @Transactional
    public Views.UserView reviewApplication(AdminCommands.ReviewApplication command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Compte introuvable"));
        if (user.getStatus() != AccountStatus.PENDING_VALIDATION) {
            throw IamException.of(IamErrorCode.APPLICATION_ALREADY_REVIEWED,
                    "Ce dossier n'est pas en attente de validation");
        }

        if (!command.approved()) {
            user.reject(clock);
            User saved = userRepository.save(user);
            notify(user, false, command.reason());
            auditLogger.success(null, AuditLog.Action.PRACTITIONER_VALIDATED,
                    "dossier #" + user.getId() + " refuse : " + command.reason());
            return viewMapper.userView(saved);
        }

        if (user.getProfileType() == ProfileType.PRACTITIONER) {
            approvePractitioner(user);
        } else if (user.getProfileType() == ProfileType.ESTABLISHMENT) {
            establishmentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Profil etablissement introuvable"));
        } else {
            throw IamException.of(IamErrorCode.UNSUPPORTED_PROFILE,
                    "Validation manuelle non prevue pour ce profil");
        }

        user.activate(clock);
        User saved = userRepository.save(user);
        notify(user, true, null);
        eventPublisher.publish(new DomainEvent.PractitionerReviewed(
                user.getId(), true, command.reason(), clock.instant()));
        auditLogger.success(null, user.getProfileType() == ProfileType.PRACTITIONER
                ? AuditLog.Action.PRACTITIONER_VALIDATED
                : AuditLog.Action.ESTABLISHMENT_VALIDATED,
                "dossier #" + user.getId() + " approuve");
        return viewMapper.userView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.KycDocumentView> pendingDocuments(int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Pagination invalide");
        }
        List<Views.KycDocumentView> docs = documentRepository.findPending(page, size).stream()
                .map(viewMapper::kycView).toList();
        return new Views.PagedResult<>(docs, docs.size(), page, size);
    }

    @Override
    @Transactional
    public Views.KycDocumentView reviewDocument(AdminCommands.ReviewDocument command) {
        KycDocument document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Document introuvable"));
        KycDocument.ReviewStatus status = command.approved()
                ? KycDocument.ReviewStatus.VERIFIED : KycDocument.ReviewStatus.REJECTED;
        document.review(status, command.note(), clock);
        KycDocument saved = documentRepository.save(document);

        if (command.approved()) {
            applyVerifiedDocument(saved);
        }
        auditLogger.success(null, AuditLog.Action.KYC_DOCUMENT_REVIEWED,
                saved.getType() + " #" + saved.getId() + " -> " + status);
        return viewMapper.kycView(saved);
    }

    private void approvePractitioner(User user) {
        PractitionerProfile profile = practitionerRepository.findByUserId(user.getId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Profil praticien introuvable"));

        List<String> missing = new ArrayList<>();
        if (!profile.isRppsVerified()) {
            missing.add("RPPS verifie");
        }
        if (!profile.isDiplomaVerified()) {
            missing.add("diplome verifie");
        }
        if (!profile.isProfessionalInsuranceVerified()) {
            missing.add("assurance responsabilite civile");
        }
        if (!profile.isBankAccountVerified()) {
            missing.add("RIB verifie");
        }
        if (!contractRepository.existsAcceptedByPractitionerUserId(user.getId())) {
            missing.add("contrat d'adhesion signe");
        }
        if (!missing.isEmpty()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Dossier incomplet, elements manquants : " + String.join(", ", missing));
        }
    }

    /** Met a jour les fanions de verification du profil concerne par le document. */
    private void applyVerifiedDocument(KycDocument document) {
        Long ownerId = document.getOwnerUserId();
        User owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            return;
        }
        if (owner.getProfileType() == ProfileType.PRACTITIONER) {
            practitionerRepository.findByUserId(ownerId).ifPresent(profile -> {
                switch (document.getType()) {
                    case DIPLOMA -> profile.setDiplomaVerified(true);
                    case PROFESSIONAL_INSURANCE -> profile.setProfessionalInsuranceVerified(true);
                    case RIB -> profile.setBankAccountVerified(true);
                    default -> { }
                }
                practitionerRepository.save(profile);
            });
        } else if (owner.getProfileType() == ProfileType.PATIENT
                && (document.getType() == KycDocument.DocumentType.IDENTITY_CARD
                    || document.getType() == KycDocument.DocumentType.PASSPORT)) {
            patientRepository.findByUserId(ownerId).ifPresent((PatientProfile patient) -> {
                patient.upgradeKyc(PatientProfile.KycLevel.VERIFIED, clock);
                patientRepository.save(patient);
            });
        }
    }

    private void notify(User user, boolean approved, String reason) {
        String subject = approved ? "Votre compte a ete valide" : "Votre dossier n'a pas ete valide";
        String body = approved
                ? "Felicitations, votre compte est desormais actif sur PRDV."
                : "Motif : " + reason + ". Contactez le support pour plus d'informations.";
        if (user.getEmail() != null) {
            notificationPort.send(user.getEmail(), subject, body);
        }
    }
}
