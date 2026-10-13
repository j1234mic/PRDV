package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.input.SocialAuthenticationUseCase;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.SocialAccountRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.TokenIssuanceSupport;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Connexion via fournisseur d'identite OAuth2/OIDC : lie le compte social a un
 * compte existant (par email verifie) ou cree automatiquement un profil patient
 * ou un compte SSO d'entreprise.
 */
@Service
public class SocialAuthenticationService implements SocialAuthenticationUseCase {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final RoleRepository roleRepository;
    private final TokenIssuanceSupport tokenIssuance;
    private final AuditLogger auditLogger;
    private final Clock clock;

    public SocialAuthenticationService(UserRepository userRepository,
                                       SocialAccountRepository socialAccountRepository,
                                       PatientProfileRepository patientProfileRepository,
                                       RoleRepository roleRepository,
                                       TokenIssuanceSupport tokenIssuance,
                                       AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.roleRepository = roleRepository;
        this.tokenIssuance = tokenIssuance;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthResults.TokenSet authenticate(SocialAccount.Provider provider, String providerUserId,
                                             String email, String firstName, String lastName,
                                             RequestMetadata metadata) {
        if (email == null || email.isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le fournisseur d'identite n'a pas fourni d'email");
        }
        RequestMetadata safeMeta = metadata == null ? RequestMetadata.unknown() : metadata;

        User user = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(link -> userRepository.findById(link.getUserId()).orElse(null))
                .orElse(null);

        if (user == null) {
            user = userRepository.findByEmail(email.toLowerCase()).orElse(null);
            if (user != null) {
                // Lie le compte existant au fournisseur social
                socialAccountRepository.save(SocialAccount.link(
                        user.getId(), provider, providerUserId, email, clock));
            }
        }

        if (user == null) {
            user = User.fromSocialProvider(email, ProfileType.PATIENT, clock);
            Role patientRole = roleRepository.findByName(Role.PATIENT)
                    .orElseThrow(() -> new IllegalStateException("Role patient amorce manquant"));
            user.addRole(patientRole);
            user = userRepository.save(user);

            PatientProfile profile = PatientProfile.create(user.getId(), firstName, lastName,
                    null, null, null, clock);
            patientProfileRepository.save(profile);

            socialAccountRepository.save(SocialAccount.link(
                    user.getId(), provider, providerUserId, email, clock));
        } else {
            // Le fournisseur OIDC garantit l'email : leve la verification si elle etait en attente
            if (user.getStatus() == AccountStatus.PENDING_EMAIL_VERIFICATION) {
                user.markEmailVerified();
                user.activate(clock);
                user = userRepository.save(user);
            }
            if (user.isCurrentlyLocked(clock)
                    || user.getStatus() == AccountStatus.SUSPENDED
                    || user.getStatus() == AccountStatus.REJECTED
                    || user.getStatus() == AccountStatus.DELETED) {
                throw IamException.of(IamErrorCode.ACCOUNT_SUSPENDED,
                        "Connexion sociale impossible pour ce compte");
            }
        }

        auditLogger.success(user.getId(), AuditLog.Action.SOCIAL_LOGIN, "connexion " + provider);
        return tokenIssuance.completeLogin(user, safeMeta, false);
    }

    @Override
    @Transactional
    public AuthResults.TokenSet authenticateEnterpriseSso(AuthCommands.EnterpriseSsoLogin command) {
        if (command.enterpriseDomain() == null || command.enterpriseDomain().isBlank()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le domaine d'entreprise SSO (enterpriseDomain) est obligatoire");
        }
        Email email = Email.of(command.email());
        String domain = command.enterpriseDomain().trim().toLowerCase();
        if (!email.value().endsWith("@" + domain)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "L'email SSO (" + email.value() + ") ne correspond pas au domaine d'entreprise (" + domain + ")");
        }
        String providerUserId = domain + ":" + command.idpSubject();
        RequestMetadata safeMeta = command.metadata() == null ? RequestMetadata.unknown() : command.metadata();

        User user = socialAccountRepository.findByProviderAndProviderUserId(
                        SocialAccount.Provider.OIDC_ENTERPRISE, providerUserId)
                .map(link -> userRepository.findById(link.getUserId()).orElse(null))
                .orElseGet(() -> userRepository.findByEmail(email.value()).orElse(null));

        if (user == null) {
            ProfileType profileType = command.profileType() == null
                    ? ProfileType.PATIENT : command.profileType();
            user = User.fromSocialProvider(email.value(), profileType, clock);
            String defaultRole = switch (profileType) {
                case PRACTITIONER -> Role.PRACTITIONER;
                case SECRETARY -> Role.SECRETARY;
                case ESTABLISHMENT -> Role.ESTABLISHMENT;
                case ADMIN -> Role.TECHNICAL_SUPPORT;
                case PATIENT -> Role.PATIENT;
            };
            roleRepository.findByName(defaultRole).ifPresent(user::addRole);
            user = userRepository.save(user);
            if (profileType == ProfileType.PATIENT) {
                patientProfileRepository.save(PatientProfile.create(
                        user.getId(), command.firstName(), command.lastName(), null, null, null, clock));
            }
            socialAccountRepository.save(SocialAccount.link(
                    user.getId(), SocialAccount.Provider.OIDC_ENTERPRISE, providerUserId, email.value(), clock));
        }

        user.assertCanAuthenticate(clock);
        auditLogger.success(user.getId(), AuditLog.Action.SSO_LOGIN,
                "connexion SSO entreprise (" + domain + ")");
        return tokenIssuance.completeLogin(user, safeMeta, false);
    }
}
