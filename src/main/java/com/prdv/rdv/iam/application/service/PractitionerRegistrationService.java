package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.KycDocumentsUseCase;
import com.prdv.rdv.iam.application.port.input.PractitionerRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.BankAccountVerificationPort;
import com.prdv.rdv.iam.application.port.output.ContractRepository;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.MedicalRegistryPort;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.PractitionerProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class PractitionerRegistrationService implements PractitionerRegistrationUseCase {

    private static final String NS_IBAN = "IBAN";

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final PractitionerProfileRepository profileRepository;
    private final MedicalRegistryPort medicalRegistry;
    private final BankAccountVerificationPort bankVerification;
    private final TokenizationPort tokenization;
    private final KycDocumentsUseCase kycDocumentsUseCase;
    private final ContractRepository contractRepository;
    private final MembershipRepository membershipRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public PractitionerRegistrationService(RegistrationSupport registrationSupport,
                                           UserRepository userRepository,
                                           PractitionerProfileRepository profileRepository,
                                           MedicalRegistryPort medicalRegistry,
                                           BankAccountVerificationPort bankVerification,
                                           TokenizationPort tokenization,
                                           KycDocumentsUseCase kycDocumentsUseCase,
                                           ContractRepository contractRepository,
                                           MembershipRepository membershipRepository,
                                           SecurityContextPort securityContext,
                                           ViewMapper viewMapper, AuditLogger auditLogger,
                                           DomainEventPublisher eventPublisher, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.medicalRegistry = medicalRegistry;
        this.bankVerification = bankVerification;
        this.tokenization = tokenization;
        this.kycDocumentsUseCase = kycDocumentsUseCase;
        this.contractRepository = contractRepository;
        this.membershipRepository = membershipRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.OtpSentView register(RegistrationCommands.RegisterPractitioner cmd) {
        User user = registrationSupport.createAccount(
                cmd.email(), cmd.phone(), cmd.password(), ProfileType.PRACTITIONER, Role.PRACTITIONER);

        // Verification automatique RPPS / ADELI / diplomes via l'API des referentiels medicaux
        MedicalRegistryPort.LicenseVerification license =
                medicalRegistry.verify(cmd.rppsNumber(), cmd.adeliNumber(), cmd.lastName(), cmd.firstName());

        PractitionerProfile profile = PractitionerProfile.create(user.getId(), cmd.firstName(),
                cmd.lastName(), cmd.specialty(), cmd.rppsNumber(), cmd.adeliNumber(), clock);
        profile.setRppsVerified(license.rppsValid());
        profile.setAdeliVerified(license.adeliValid());
        profile.setDiplomaVerified(license.diplomaValid());

        if (cmd.iban() != null && !cmd.iban().isBlank()) {
            BankAccountVerificationPort.BankCheck bankCheck =
                    bankVerification.verify(cmd.iban(), cmd.lastName() + " " + cmd.firstName());
            if (!bankCheck.valid()) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Le RIB/IBAN n'a pas pu etre verifie : " + bankCheck.reason());
            }
            profile.setBankAccountVerified(true);
            profile.storeBankDetails(tokenization.tokenize(cmd.iban(), NS_IBAN),
                    tokenization.mask(cmd.iban()));
        }

        profileRepository.save(profile);
        Views.OtpSentView otp = registrationSupport.sendContactOtp(user);
        eventPublisher.publish(new DomainEvent.PractitionerApplicationSubmitted(
                user.getId(), user.getEmail(), cmd.rppsNumber(), clock.instant()));
        return otp;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PractitionerView currentProfile() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        PractitionerProfile profile = requireProfile(userId);
        return viewMapper.practitionerView(user, profile);
    }

    @Override
    @Transactional
    public Views.KycDocumentView uploadDocument(ProfileCommands.UploadKycDocument command) {
        Long userId = securityContext.requireCurrentUserId();
        Views.KycDocumentView view = kycDocumentsUseCase.upload(new ProfileCommands.UploadKycDocument(
                userId, command.type(), command.originalFilename(), command.contentType(), command.content()));
        auditLogger.record(userId, AuditLog.Action.KYC_DOCUMENT_UPLOADED, AuditLog.Outcome.SUCCESS,
                "KycDocument", String.valueOf(view.id()), command.type().name(), null);
        return view;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.KycDocumentView> myDocuments() {
        return kycDocumentsUseCase.listForOwner(securityContext.requireCurrentUserId());
    }

    @Override
    @Transactional
    public Views.ContractView acceptContract(ProfileCommands.AcceptContract command) {
        Long userId = securityContext.requireCurrentUserId();
        requireProfile(userId);
        PractitionerContract contract = PractitionerContract.accept(
                userId, command.version(), command.contentHash(), command.ipAddress(), clock);
        contractRepository.save(contract);
        auditLogger.success(userId, AuditLog.Action.USER_REGISTERED,
                "contrat d'adhesion accepte (version " + command.version() + ")");
        return viewMapper.contractView(contract);
    }

    @Override
    @Transactional
    public Views.MembershipView requestMembership(ProfileCommands.RequestMembership command) {
        Long practitionerId = securityContext.requireCurrentUserId();
        requireProfile(practitionerId);
        User establishment = requireEstablishment(command.establishmentUserId());

        boolean alreadyLinked = membershipRepository.findByPractitionerUserId(practitionerId).stream()
                .anyMatch(m -> m.getEstablishmentUserId().equals(establishment.getId())
                        && (m.getStatus() == EstablishmentMembership.MembershipStatus.PENDING
                            || m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE));
        if (alreadyLinked) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Un rattachement existe deja avec cet etablissement");
        }

        EstablishmentMembership membership = EstablishmentMembership.request(
                establishment.getId(), practitionerId, command.role(),
                command.validFrom(), command.validUntil(), clock);
        return viewMapper.membershipView(membershipRepository.save(membership));
    }

    @Override
    @Transactional
    public Views.MembershipView declareReplacement(ProfileCommands.DeclareReplacement command) {
        Long practitionerId = securityContext.requireCurrentUserId();
        requireProfile(practitionerId);
        requireEstablishment(command.establishmentUserId());
        EstablishmentMembership membership = EstablishmentMembership.request(
                command.establishmentUserId(), practitionerId,
                EstablishmentMembership.MemberRole.REPLACER,
                command.validFrom(), command.validUntil(), clock);
        return viewMapper.membershipView(membershipRepository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.MembershipView> myMemberships() {
        Long userId = securityContext.requireCurrentUserId();
        return membershipRepository.findByPractitionerUserId(userId).stream()
                .map(viewMapper::membershipView).toList();
    }

    private PractitionerProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil praticien introuvable"));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }

    private User requireEstablishment(Long establishmentUserId) {
        User establishment = userRepository.findById(establishmentUserId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Etablissement introuvable"));
        if (establishment.getProfileType() != ProfileType.ESTABLISHMENT) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le compte cible n'est pas un etablissement");
        }
        return establishment;
    }
}
