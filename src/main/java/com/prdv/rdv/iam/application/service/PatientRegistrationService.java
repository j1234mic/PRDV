package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.KycDocumentsUseCase;
import com.prdv.rdv.iam.application.port.input.PatientRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class PatientRegistrationService implements PatientRegistrationUseCase {

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final KycDocumentsUseCase kycDocumentsUseCase;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public PatientRegistrationService(RegistrationSupport registrationSupport,
                                      UserRepository userRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      KycDocumentsUseCase kycDocumentsUseCase,
                                      SecurityContextPort securityContext,
                                      ViewMapper viewMapper,
                                      DomainEventPublisher eventPublisher, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.kycDocumentsUseCase = kycDocumentsUseCase;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PatientRegistrationService(RegistrationSupport registrationSupport,
                                      UserRepository userRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      KycDocumentsUseCase kycDocumentsUseCase,
                                      SecurityContextPort securityContext,
                                      DomainEventPublisher eventPublisher, Clock clock) {
        this(registrationSupport, userRepository, patientProfileRepository,
                kycDocumentsUseCase, securityContext, new ViewMapper(), eventPublisher, clock);
    }

    @Override
    @Transactional
    public Views.OtpSentView register(RegistrationCommands.RegisterPatient cmd) {
        Long guardianUserId = cmd.guardianUserId();
        if (guardianUserId != null) {
            User guardian = userRepository.findById(guardianUserId)
                    .filter(u -> u.getProfileType() == ProfileType.PATIENT)
                    .orElseThrow(() -> IamException.of(IamErrorCode.VALIDATION_ERROR,
                            "Le tuteur/parent rattache est introuvable"));
            if (guardian.getStatus() != AccountStatus.ACTIVE) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Le compte du tuteur doit etre actif pour rattacher un mineur");
            }
        }

        User user = registrationSupport.createAccount(
                cmd.email(), cmd.phone(), cmd.password(), ProfileType.PATIENT, Role.PATIENT);

        PatientProfile profile = PatientProfile.create(user.getId(), cmd.firstName(), cmd.lastName(),
                cmd.birthDate(), cmd.gender(), guardianUserId, clock);
        if (profile.isMinor(clock) && guardianUserId == null) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Un patient mineur doit etre rattache a un parent/tuteur");
        }
        patientProfileRepository.save(profile);

        Views.OtpSentView otp = registrationSupport.sendContactOtp(user);
        eventPublisher.publish(new DomainEvent.PatientRegistered(user.getId(), user.getEmail(),
                clock.instant()));
        return otp;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PatientView currentProfile() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        PatientProfile profile = requireProfile(userId);
        return viewMapper.patientView(user, profile);
    }

    @Override
    @Transactional
    public Views.PatientView registerMinor(RegistrationCommands.RegisterMinorPatient command) {
        Long guardianId = securityContext.requireCurrentUserId();
        User guardian = requireUser(guardianId);
        if (guardian.getProfileType() != ProfileType.PATIENT) {
            throw IamException.of(IamErrorCode.PROFILE_MISMATCH,
                    "Seul un compte patient parent/tuteur peut rattacher un profil mineur");
        }
        if (guardian.getStatus() != AccountStatus.ACTIVE) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le compte du tuteur doit etre actif pour rattacher un mineur");
        }

        String childEmail = (command.email() == null || command.email().isBlank())
                ? "minor." + guardianId + "." + System.nanoTime() + "@family.prdv.local"
                : command.email();
        String childPassword = (command.password() == null || command.password().isBlank())
                ? "Minor#" + guardianId + "A9z!"
                : command.password();

        User childUser = registrationSupport.createAccount(
                childEmail, command.phone(), childPassword, ProfileType.PATIENT, Role.PATIENT);
        // Le parent/tuteur etant deja authentifie et verifie, le profil mineur rattache est actif
        childUser.markEmailVerified();
        childUser.activate(clock);
        childUser = userRepository.save(childUser);

        PatientProfile childProfile = PatientProfile.create(childUser.getId(),
                command.firstName(), command.lastName(), command.birthDate(),
                command.gender(), guardianId, clock);
        if (!childProfile.isMinor(clock)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La date de naissance indique que le patient est majeur (>= 18 ans)");
        }
        childProfile.upgradeKyc(PatientProfile.KycLevel.BASIC, clock);
        childProfile = patientProfileRepository.save(childProfile);

        eventPublisher.publish(new DomainEvent.PatientRegistered(
                childUser.getId(), childUser.getEmail(), clock.instant()));
        return viewMapper.patientView(childUser, childProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.PatientView> listMyMinors() {
        Long guardianId = securityContext.requireCurrentUserId();
        return patientProfileRepository.findByGuardianUserId(guardianId).stream()
                .flatMap(profile -> userRepository.findById(profile.getUserId()).stream()
                        .map(user -> viewMapper.patientView(user, profile)))
                .toList();
    }

    @Override
    @Transactional
    public Views.KycDocumentView uploadIdentityDocument(ProfileCommands.UploadKycDocument command) {
        Long ownerId = securityContext.requireCurrentUserId();
        return kycDocumentsUseCase.upload(new ProfileCommands.UploadKycDocument(
                ownerId, command.type(), command.originalFilename(),
                command.contentType(), command.content()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.KycDocumentView> myDocuments() {
        return kycDocumentsUseCase.listForOwner(securityContext.requireCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PatientKycStatusView myKycStatus() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        PatientProfile profile = requireProfile(userId);
        boolean idVerified = profile.getIdentityDocumentToken() != null
                && !profile.getIdentityDocumentToken().isBlank()
                && profile.getKycLevel() == PatientProfile.KycLevel.VERIFIED;
        return new Views.PatientKycStatusView(
                userId,
                profile.getKycLevel(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                idVerified,
                profile.getIdentityDocumentToken(),
                profile.isEligibleForPayment(),
                profile.missingKycRequirements(user.isEmailVerified(), user.isPhoneVerified()));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }

    private PatientProfile requireProfile(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil patient introuvable"));
    }
}
