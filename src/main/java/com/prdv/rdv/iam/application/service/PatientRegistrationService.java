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
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class PatientRegistrationService implements PatientRegistrationUseCase {

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final KycDocumentsUseCase kycDocumentsUseCase;
    private final SecurityContextPort securityContext;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public PatientRegistrationService(RegistrationSupport registrationSupport,
                                      UserRepository userRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      KycDocumentsUseCase kycDocumentsUseCase,
                                      SecurityContextPort securityContext,
                                      DomainEventPublisher eventPublisher, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.kycDocumentsUseCase = kycDocumentsUseCase;
        this.securityContext = securityContext;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
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
            if (guardian.getStatus() != com.prdv.rdv.iam.domain.model.user.AccountStatus.ACTIVE) {
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
    @Transactional
    public Views.KycDocumentView uploadIdentityDocument(ProfileCommands.UploadKycDocument command) {
        Long ownerId = securityContext.requireCurrentUserId();
        return kycDocumentsUseCase.upload(new ProfileCommands.UploadKycDocument(
                ownerId, command.type(), command.originalFilename(),
                command.contentType(), command.content()));
    }
}
