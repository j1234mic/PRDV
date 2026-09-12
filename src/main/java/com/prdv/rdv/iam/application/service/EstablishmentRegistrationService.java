package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.EstablishmentRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class EstablishmentRegistrationService implements EstablishmentRegistrationUseCase {

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final EstablishmentProfileRepository profileRepository;
    private final MembershipRepository membershipRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public EstablishmentRegistrationService(RegistrationSupport registrationSupport,
                                            UserRepository userRepository,
                                            EstablishmentProfileRepository profileRepository,
                                            MembershipRepository membershipRepository,
                                            SecurityContextPort securityContext,
                                            ViewMapper viewMapper, AuditLogger auditLogger,
                                            DomainEventPublisher eventPublisher, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.membershipRepository = membershipRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.OtpSentView register(RegistrationCommands.RegisterEstablishment cmd) {
        User user = registrationSupport.createAccount(
                cmd.email(), cmd.phone(), cmd.password(), ProfileType.ESTABLISHMENT, Role.ESTABLISHMENT);

        EstablishmentProfile profile = EstablishmentProfile.create(user.getId(),
                cmd.legalName(), cmd.siret(), cmd.address(), cmd.departments(), clock);
        profileRepository.save(profile);

        Views.OtpSentView otp = registrationSupport.sendContactOtp(user);
        eventPublisher.publish(new DomainEvent.EstablishmentApplicationSubmitted(
                user.getId(), cmd.legalName(), clock.instant()));
        return otp;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.EstablishmentView currentProfile() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        EstablishmentProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil etablissement introuvable"));
        return viewMapper.establishmentView(user, profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.MembershipView> memberships() {
        Long userId = securityContext.requireCurrentUserId();
        return membershipRepository.findByEstablishmentUserId(userId).stream()
                .map(viewMapper::membershipView).toList();
    }

    @Override
    @Transactional
    public Views.MembershipView reviewMembership(ProfileCommands.ReviewMembership command) {
        Long establishmentId = securityContext.requireCurrentUserId();
        EstablishmentMembership membership = membershipRepository.findById(command.membershipId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Rattachement introuvable"));
        if (!membership.getEstablishmentUserId().equals(establishmentId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN, "Rattachement qui ne concerne pas cet etablissement");
        }
        if (membership.getStatus() != EstablishmentMembership.MembershipStatus.PENDING) {
            throw IamException.of(IamErrorCode.APPLICATION_ALREADY_REVIEWED, "Rattachement deja traite");
        }
        if (command.approved()) {
            membership.accept(clock);
        } else {
            membership.reject(clock);
        }
        auditLogger.success(establishmentId, AuditLog.Action.PRACTITIONER_VALIDATED,
                "rattachement " + (command.approved() ? "accepte" : "refuse") + " #" + membership.getId());
        return viewMapper.membershipView(membershipRepository.save(membership));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }
}
