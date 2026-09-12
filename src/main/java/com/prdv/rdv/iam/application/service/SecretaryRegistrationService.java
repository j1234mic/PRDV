package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.SecretaryRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.SecretaryProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;

@Service
public class SecretaryRegistrationService implements SecretaryRegistrationUseCase {

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final SecretaryProfileRepository secretaryProfileRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SecretaryRegistrationService(RegistrationSupport registrationSupport,
                                        UserRepository userRepository,
                                        SecretaryProfileRepository secretaryProfileRepository,
                                        SecurityContextPort securityContext,
                                        ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.secretaryProfileRepository = secretaryProfileRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.SecretaryView create(RegistrationCommands.RegisterSecretary command) {
        Long creatorId = securityContext.requireCurrentUserId();

        Set<Long> practitionerIds = command.supervisedPractitionerIds() == null
                ? Set.of() : command.supervisedPractitionerIds();
        for (Long practitionerId : practitionerIds) {
            User practitioner = userRepository.findById(practitionerId)
                    .orElseThrow(() -> IamException.of(IamErrorCode.VALIDATION_ERROR,
                            "Praticien rattache introuvable : " + practitionerId));
            if (practitioner.getProfileType() != ProfileType.PRACTITIONER) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Le compte " + practitionerId + " n'est pas un praticien");
            }
        }

        // Pas de mot de passe choisi par le createur : un mot de passe aleatoire temporaire est
        // cree (il devra etre change via le parcours OTP / mot de passe oublie) ;
        // la creation envoie un email d'invitation.
        String temporaryPassword = "W3lcome#" + System.nanoTime();
        User user = registrationSupport.createAccount(
                command.email(), null, temporaryPassword, ProfileType.SECRETARY, Role.SECRETARY);

        // Permissions granulaires attribuees directement (ex : appointment.write sans billing.read)
        if (command.permissionCodes() != null) {
            user.getDirectPermissions().addAll(command.permissionCodes());
        }
        userRepository.save(user);

        SecretaryProfile profile = SecretaryProfile.create(user.getId(), command.firstName(),
                command.lastName(), practitionerIds, clock);
        secretaryProfileRepository.save(profile);

        registrationSupport.sendContactOtp(user);
        auditLogger.record(creatorId, AuditLog.Action.USER_REGISTERED, AuditLog.Outcome.SUCCESS,
                "Secretary", String.valueOf(user.getId()),
                "secretaire creee et rattachee a " + practitionerIds.size() + " praticien(s)", null);

        User reloaded = userRepository.findById(user.getId()).orElse(user);
        return viewMapper.secretaryView(reloaded, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.SecretaryView> listForPractitioner(Long practitionerUserId) {
        return secretaryProfileRepository.findBySupervisedPractitionerId(practitionerUserId).stream()
                .map(profile -> viewMapper.secretaryView(
                        userRepository.findById(profile.getUserId())
                                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable")),
                        profile))
                .toList();
    }
}
