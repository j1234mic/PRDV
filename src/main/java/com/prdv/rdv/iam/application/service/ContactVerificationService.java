package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.port.input.ContactVerificationUseCase;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.OtpIssuer;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Confirmation OTP au moment de l'inscription, valable pour tous les profils.
 */
@Service
public class ContactVerificationService implements ContactVerificationUseCase {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final OtpIssuer otpIssuer;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public ContactVerificationService(UserRepository userRepository,
                                      PatientProfileRepository patientProfileRepository,
                                      OtpIssuer otpIssuer,
                                      ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.otpIssuer = otpIssuer;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.UserView confirmRegistrationOtp(AuthCommands.VerifyRegistrationOtp command) {
        String target = command.target().trim().toLowerCase();
        User user = userRepository.findByEmail(target)
                .or(() -> userRepository.findByPhone(command.target().trim()))
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Aucun compte avec cette destination"));

        if (user.getStatus() != AccountStatus.PENDING_EMAIL_VERIFICATION) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Aucune verification en attente pour ce compte");
        }

        otpIssuer.verifyLatest(target, OtpChallenge.Purpose.REGISTRATION, command.code());

        if (command.channel() == OtpChallenge.Channel.EMAIL) {
            user.markEmailVerified();
        } else {
            user.markPhoneVerified();
        }
        user.setStatus(RegistrationSupport.statusAfterOtp(user.getProfileType()));
        User saved = userRepository.save(user);

        // Niveau KYC BASIQUE pour le patient dont les coordonnees sont verifiees
        if (user.getProfileType() == ProfileType.PATIENT) {
            patientProfileRepository.findByUserId(user.getId()).ifPresent((PatientProfile patient) -> {
                patient.upgradeKyc(PatientProfile.KycLevel.BASIC, clock);
                patientProfileRepository.save(patient);
            });
        }

        auditLogger.success(user.getId(), AuditLog.Action.OTP_VERIFIED,
                command.channel() + " verifie lors de l'inscription");
        return viewMapper.userView(saved);
    }

    @Override
    @Transactional
    public Views.OtpSentView resend(AuthCommands.ResendOtp command) {
        String target = command.target().trim().toLowerCase();
        User user = userRepository.findByEmail(target)
                .or(() -> userRepository.findByPhone(command.target().trim()))
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Compte introuvable"));

        if (user.getStatus() != AccountStatus.PENDING_EMAIL_VERIFICATION) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Aucun code en attente pour ce compte");
        }
        otpIssuer.assertResendCooldown(target, command.purpose());
        Views.OtpSentView view = otpIssuer.issue(target, command.channel(), command.purpose());
        auditLogger.success(user.getId(), AuditLog.Action.OTP_REQUESTED, "renvoi OTP " + command.purpose());
        return view;
    }
}
