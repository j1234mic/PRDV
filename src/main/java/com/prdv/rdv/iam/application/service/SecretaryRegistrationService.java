package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.SecretaryRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.SecretaryProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SecretaryRegistrationService implements SecretaryRegistrationUseCase {

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final SecretaryProfileRepository secretaryProfileRepository;
    private final MembershipRepository membershipRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    @Autowired
    public SecretaryRegistrationService(RegistrationSupport registrationSupport,
                                        UserRepository userRepository,
                                        SecretaryProfileRepository secretaryProfileRepository,
                                        MembershipRepository membershipRepository,
                                        AuditLogRepository auditLogRepository,
                                        SecurityContextPort securityContext,
                                        ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.secretaryProfileRepository = secretaryProfileRepository;
        this.membershipRepository = membershipRepository;
        this.auditLogRepository = auditLogRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    public SecretaryRegistrationService(RegistrationSupport registrationSupport,
                                        UserRepository userRepository,
                                        SecretaryProfileRepository secretaryProfileRepository,
                                        SecurityContextPort securityContext,
                                        ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this(registrationSupport, userRepository, secretaryProfileRepository,
                null, null, securityContext, viewMapper, auditLogger, clock);
    }

    @Override
    @Transactional
    public Views.SecretaryView create(RegistrationCommands.RegisterSecretary command) {
        Views.SecretaryDetailView detail = createWithMultiCabinet(new RegistrationCommands.RegisterSecretaryMultiCabinet(
                command.email(), command.firstName(), command.lastName(),
                command.supervisedPractitionerIds(), Set.of(), command.permissionCodes()));
        return new Views.SecretaryView(detail.user(), detail.firstName(),
                detail.lastName(), detail.supervisedPractitionerIds());
    }

    @Override
    @Transactional
    public Views.SecretaryDetailView createWithMultiCabinet(RegistrationCommands.RegisterSecretaryMultiCabinet command) {
        Long creatorId = securityContext.requireCurrentUserId();

        Set<Long> practitionerIds = command.supervisedPractitionerIds() == null
                ? Set.of() : command.supervisedPractitionerIds();
        validatePractitioners(practitionerIds);

        Set<Long> establishmentIds = new HashSet<>();
        if (command.accessibleEstablishmentIds() != null) {
            validateEstablishments(command.accessibleEstablishmentIds());
            establishmentIds.addAll(command.accessibleEstablishmentIds());
        }
        // Acces multi-cabinets : deduit egalement les cabinets actifs des praticiens supervises
        if (membershipRepository != null) {
            for (Long practitionerId : practitionerIds) {
                membershipRepository.findByPractitionerUserId(practitionerId).stream()
                        .filter(m -> m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE)
                        .map(EstablishmentMembership::getEstablishmentUserId)
                        .forEach(establishmentIds::add);
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
                command.lastName(), practitionerIds, establishmentIds, clock);
        secretaryProfileRepository.save(profile);

        registrationSupport.sendContactOtp(user);
        auditLogger.record(creatorId, AuditLog.Action.USER_REGISTERED, AuditLog.Outcome.SUCCESS,
                "Secretary", String.valueOf(user.getId()),
                "secretaire creee et rattachee a " + practitionerIds.size() + " praticien(s) et "
                        + establishmentIds.size() + " cabinet(s)", null);

        User reloaded = userRepository.findById(user.getId()).orElse(user);
        return viewMapper.secretaryDetailView(reloaded, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.SecretaryDetailView currentProfile() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        SecretaryProfile profile = requireProfile(userId);
        return viewMapper.secretaryDetailView(user, profile);
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

    @Override
    @Transactional
    public Views.SecretaryDetailView updatePermissions(ProfileCommands.UpdateSecretaryPermissions command) {
        Long actorId = securityContext.requireCurrentUserId();
        User secretaryUser = requireUser(command.secretaryUserId());
        SecretaryProfile profile = requireProfile(command.secretaryUserId());

        secretaryUser.setDirectPermissions(command.permissionCodes() == null
                ? new HashSet<>() : new HashSet<>(command.permissionCodes()));
        User saved = userRepository.save(secretaryUser);
        auditLogger.record(actorId, AuditLog.Action.SECRETARY_PERMISSIONS_UPDATED, AuditLog.Outcome.SUCCESS,
                "Secretary", String.valueOf(saved.getId()),
                "permissions granulaires -> " + saved.getDirectPermissions(), null);
        return viewMapper.secretaryDetailView(saved, profile);
    }

    @Override
    @Transactional
    public Views.SecretaryDetailView updateAssignments(ProfileCommands.UpdateSecretaryAssignments command) {
        Long actorId = securityContext.requireCurrentUserId();
        User secretaryUser = requireUser(command.secretaryUserId());
        SecretaryProfile profile = requireProfile(command.secretaryUserId());

        if (command.supervisedPractitionerIds() != null) {
            validatePractitioners(command.supervisedPractitionerIds());
            profile.assignPractitioners(command.supervisedPractitionerIds(), clock);
        }
        if (command.accessibleEstablishmentIds() != null) {
            validateEstablishments(command.accessibleEstablishmentIds());
            profile.assignEstablishments(command.accessibleEstablishmentIds(), clock);
        }
        SecretaryProfile savedProfile = secretaryProfileRepository.save(profile);
        auditLogger.record(actorId, AuditLog.Action.SECRETARY_CABINETS_UPDATED, AuditLog.Outcome.SUCCESS,
                "Secretary", String.valueOf(secretaryUser.getId()),
                "praticiens=" + savedProfile.getSupervisedPractitionerIds()
                        + ", cabinets=" + savedProfile.getAccessibleEstablishmentIds(), null);
        return viewMapper.secretaryDetailView(secretaryUser, savedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.AuditView> secretaryAuditTrail(Long secretaryUserId, int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Pagination invalide");
        }
        requireProfile(secretaryUserId);
        if (auditLogRepository == null) {
            return new Views.PagedResult<>(List.of(), 0, page, size);
        }
        List<Views.AuditView> items = auditLogRepository.findByUserId(secretaryUserId, page, size).stream()
                .map(viewMapper::auditView)
                .toList();
        return new Views.PagedResult<>(items, items.size(), page, size);
    }

    private void validatePractitioners(Set<Long> practitionerIds) {
        for (Long practitionerId : practitionerIds) {
            User practitioner = userRepository.findById(practitionerId)
                    .orElseThrow(() -> IamException.of(IamErrorCode.VALIDATION_ERROR,
                            "Praticien rattache introuvable : " + practitionerId));
            if (practitioner.getProfileType() != ProfileType.PRACTITIONER) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Le compte " + practitionerId + " n'est pas un praticien");
            }
        }
    }

    private void validateEstablishments(Set<Long> establishmentIds) {
        for (Long establishmentId : establishmentIds) {
            User establishment = userRepository.findById(establishmentId)
                    .orElseThrow(() -> IamException.of(IamErrorCode.VALIDATION_ERROR,
                            "Cabinet / etablissement introuvable : " + establishmentId));
            if (establishment.getProfileType() != ProfileType.ESTABLISHMENT) {
                throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                        "Le compte " + establishmentId + " n'est pas un etablissement");
            }
        }
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }

    private SecretaryProfile requireProfile(Long userId) {
        return secretaryProfileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil secretaire introuvable"));
    }
}
