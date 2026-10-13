package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.EstablishmentRegistrationUseCase;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.application.port.output.GroupInvoiceRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.SecretaryProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
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
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class EstablishmentRegistrationService implements EstablishmentRegistrationUseCase {

    private static final long DEFAULT_PLATFORM_FEE_BPS = 300L; // 3.00%

    private final RegistrationSupport registrationSupport;
    private final UserRepository userRepository;
    private final EstablishmentProfileRepository profileRepository;
    private final MembershipRepository membershipRepository;
    private final GroupInvoiceRepository groupInvoiceRepository;
    private final SecretaryProfileRepository secretaryProfileRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public EstablishmentRegistrationService(RegistrationSupport registrationSupport,
                                            UserRepository userRepository,
                                            EstablishmentProfileRepository profileRepository,
                                            MembershipRepository membershipRepository,
                                            GroupInvoiceRepository groupInvoiceRepository,
                                            SecretaryProfileRepository secretaryProfileRepository,
                                            SecurityContextPort securityContext,
                                            ViewMapper viewMapper, AuditLogger auditLogger,
                                            DomainEventPublisher eventPublisher, Clock clock) {
        this.registrationSupport = registrationSupport;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.membershipRepository = membershipRepository;
        this.groupInvoiceRepository = groupInvoiceRepository;
        this.secretaryProfileRepository = secretaryProfileRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public EstablishmentRegistrationService(RegistrationSupport registrationSupport,
                                            UserRepository userRepository,
                                            EstablishmentProfileRepository profileRepository,
                                            MembershipRepository membershipRepository,
                                            SecurityContextPort securityContext,
                                            ViewMapper viewMapper, AuditLogger auditLogger,
                                            DomainEventPublisher eventPublisher, Clock clock) {
        this(registrationSupport, userRepository, profileRepository, membershipRepository,
                null, null, securityContext, viewMapper, auditLogger, eventPublisher, clock);
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
    public List<Views.EstablishmentView> listEstablishments() {
        return profileRepository.findAll().stream()
                .flatMap(profile -> userRepository.findById(profile.getUserId()).stream()
                        .map(user -> viewMapper.establishmentView(user, profile)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Views.EstablishmentView currentProfile() {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        EstablishmentProfile profile = requireProfile(userId);
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

    @Override
    @Transactional
    public Views.MembershipView revokeMembership(Long membershipId) {
        Long establishmentId = securityContext.requireCurrentUserId();
        EstablishmentMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Rattachement introuvable"));
        if (!membership.getEstablishmentUserId().equals(establishmentId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN, "Rattachement qui ne concerne pas cet etablissement");
        }
        membership.revoke(clock);
        EstablishmentMembership saved = membershipRepository.save(membership);
        auditLogger.success(establishmentId, AuditLog.Action.PRACTITIONER_VALIDATED,
                "rattachement revoque #" + membership.getId());
        return viewMapper.membershipView(saved);
    }

    @Override
    @Transactional
    public Views.EstablishmentView updateDepartments(ProfileCommands.UpdateDepartments command) {
        Long userId = securityContext.requireCurrentUserId();
        User user = requireUser(userId);
        EstablishmentProfile profile = requireProfile(userId);
        profile.updateDepartments(command.departments(), clock);
        EstablishmentProfile saved = profileRepository.save(profile);
        auditLogger.success(userId, AuditLog.Action.ESTABLISHMENT_DEPARTMENTS_UPDATED,
                "departements mis a jour : " + saved.getDepartments());
        return viewMapper.establishmentView(user, saved);
    }

    /** Convertit la commission saisie en pourcentage (0 a 100) en points de base ; {@code null} = defaut. */
    private static long resolveFeeBasisPoints(Integer ratePercent) {
        if (ratePercent == null) {
            return DEFAULT_PLATFORM_FEE_BPS;
        }
        if (ratePercent < 0 || ratePercent > 100) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le taux de commission doit etre compris entre 0 et 100 %");
        }
        return ratePercent * 100L;
    }

    @Override
    @Transactional
    public Views.GroupInvoiceView issueGroupInvoice(ProfileCommands.IssueGroupInvoice command) {
        Long establishmentId = securityContext.requireCurrentUserId();
        requireProfile(establishmentId);
        if (groupInvoiceRepository == null) {
            throw IamException.of(IamErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                    "Depot de facturation groupee non initialise");
        }

        Set<Long> practitionerIds = new HashSet<>();
        if (command.practitionerUserIds() != null && !command.practitionerUserIds().isEmpty()) {
            practitionerIds.addAll(command.practitionerUserIds());
        } else {
            membershipRepository.findByEstablishmentUserId(establishmentId).stream()
                    .filter(m -> m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE)
                    .map(EstablishmentMembership::getPractitionerUserId)
                    .forEach(practitionerIds::add);
        }

        long feeBasisPoints = resolveFeeBasisPoints(command.platformFeeRatePercent());
        String invoiceNumber = "INV-" + establishmentId + "-" + clock.instant().toEpochMilli();
        GroupInvoice invoice = GroupInvoice.issue(
                establishmentId,
                invoiceNumber,
                command.periodStart(),
                command.periodEnd(),
                command.department(),
                practitionerIds,
                command.consultationsCount(),
                command.totalAmountCents(),
                feeBasisPoints,
                clock);
        GroupInvoice saved = groupInvoiceRepository.save(invoice);
        auditLogger.success(establishmentId, AuditLog.Action.GROUP_INVOICE_CREATED,
                "facture groupee " + saved.getInvoiceNumber() + " (" + saved.getTotalAmountCents() + " cts)");
        eventPublisher.publish(new DomainEvent.GroupInvoiceIssued(
                establishmentId, saved.getInvoiceNumber(), saved.getTotalAmountCents(), clock.instant()));
        return viewMapper.groupInvoiceView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.GroupInvoiceView> listGroupInvoices() {
        Long establishmentId = securityContext.requireCurrentUserId();
        if (groupInvoiceRepository == null) {
            return List.of();
        }
        return groupInvoiceRepository.findByEstablishmentUserId(establishmentId).stream()
                .map(viewMapper::groupInvoiceView).toList();
    }

    @Override
    @Transactional
    public Views.GroupInvoiceView payGroupInvoice(Long invoiceId) {
        Long establishmentId = securityContext.requireCurrentUserId();
        if (groupInvoiceRepository == null) {
            throw IamException.of(IamErrorCode.NOT_FOUND, "Facture groupee introuvable");
        }
        GroupInvoice invoice = groupInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Facture groupee introuvable"));
        if (!invoice.getEstablishmentUserId().equals(establishmentId)) {
            throw IamException.of(IamErrorCode.FORBIDDEN, "Facture appartenant a un autre etablissement");
        }
        invoice.markPaid(clock);
        GroupInvoice saved = groupInvoiceRepository.save(invoice);
        auditLogger.success(establishmentId, AuditLog.Action.GROUP_INVOICE_PAID,
                "facture groupee reglee " + saved.getInvoiceNumber());
        return viewMapper.groupInvoiceView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.EstablishmentDashboardView consolidatedDashboard() {
        Long establishmentId = securityContext.requireCurrentUserId();
        User user = requireUser(establishmentId);
        EstablishmentProfile profile = requireProfile(establishmentId);
        Views.EstablishmentView establishmentView = viewMapper.establishmentView(user, profile);

        List<EstablishmentMembership> memberships = membershipRepository.findByEstablishmentUserId(establishmentId);
        int activePractitioners = (int) memberships.stream()
                .filter(m -> m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE
                        && m.getMemberRole() != EstablishmentMembership.MemberRole.REPLACER)
                .count();
        int activeReplacers = (int) memberships.stream()
                .filter(m -> m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE
                        && m.getMemberRole() == EstablishmentMembership.MemberRole.REPLACER)
                .count();
        int pendingMemberships = (int) memberships.stream()
                .filter(m -> m.getStatus() == EstablishmentMembership.MembershipStatus.PENDING)
                .count();

        Set<Long> secretaryUserIds = new HashSet<>();
        if (secretaryProfileRepository != null) {
            secretaryProfileRepository.findByAccessibleEstablishmentId(establishmentId).stream()
                    .map(SecretaryProfile::getUserId)
                    .forEach(secretaryUserIds::add);
            for (EstablishmentMembership m : memberships) {
                if (m.getStatus() == EstablishmentMembership.MembershipStatus.ACTIVE) {
                    secretaryProfileRepository.findBySupervisedPractitionerId(m.getPractitionerUserId()).stream()
                            .map(SecretaryProfile::getUserId)
                            .forEach(secretaryUserIds::add);
                }
            }
        }

        List<GroupInvoice> invoices = groupInvoiceRepository == null
                ? List.of() : groupInvoiceRepository.findByEstablishmentUserId(establishmentId);
        long totalBilled = invoices.stream()
                .filter(i -> i.getStatus() != GroupInvoice.InvoiceStatus.CANCELLED)
                .mapToLong(GroupInvoice::getTotalAmountCents)
                .sum();
        long totalPaid = invoices.stream()
                .filter(i -> i.getStatus() == GroupInvoice.InvoiceStatus.PAID)
                .mapToLong(GroupInvoice::getTotalAmountCents)
                .sum();
        long pendingPayment = Math.max(0L, totalBilled - totalPaid);

        return new Views.EstablishmentDashboardView(
                establishmentView,
                profile.getDepartments().size(),
                activePractitioners,
                activeReplacers,
                pendingMemberships,
                secretaryUserIds.size(),
                invoices.size(),
                totalBilled,
                totalPaid,
                pendingPayment);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
    }

    private EstablishmentProfile requireProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil etablissement introuvable"));
    }
}
