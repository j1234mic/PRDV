package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.UserAdministrationUseCase;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.PasswordPolicy;
import com.prdv.rdv.iam.domain.model.user.PhoneNumber;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.List;

/** Gestion administrative transverse des comptes et des administrateurs systeme. */
@Service
public class UserAdministrationService implements UserAdministrationUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final UserSessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;
    private final Clock clock;

    @Autowired
    public UserAdministrationService(UserRepository userRepository,
                                     RoleRepository roleRepository,
                                     PasswordHasher passwordHasher,
                                     UserSessionRepository sessionRepository,
                                     RefreshTokenRepository refreshTokenRepository,
                                     SecurityContextPort securityContext,
                                     ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.sessionRepository = sessionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    public UserAdministrationService(UserRepository userRepository, SecurityContextPort securityContext,
                                     ViewMapper viewMapper, AuditLogger auditLogger, Clock clock) {
        this(userRepository, null, null, null, null, securityContext, viewMapper, auditLogger, clock);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.UserView> listUsers(int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Pagination invalide");
        }
        var users = userRepository.findAll(page, size);
        return new Views.PagedResult<>(viewMapper.userViews(users), userRepository.count(), page, size);
    }

    @Override
    @Transactional
    public Views.UserView changeAccountStatus(AdminCommands.ChangeAccountStatus command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        if (command.suspend()) {
            user.suspend(clock);
            auditLogger.success(securityContext.currentUserId().orElse(null),
                    AuditLog.Action.ACCOUNT_SUSPENDED, "compte #" + user.getId() + " suspendu");
        } else {
            user.activate(clock);
            auditLogger.success(securityContext.currentUserId().orElse(null),
                    AuditLog.Action.ACCOUNT_ACTIVATED, "compte #" + user.getId() + " reactive");
        }
        return viewMapper.userView(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Views.UserView getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        return viewMapper.userView(user);
    }

    @Override
    @Transactional
    public Views.UserView unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        user.unlock(clock);
        User saved = userRepository.save(user);
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.ACCOUNT_ACTIVATED, "compte #" + userId + " deverrouille");
        return viewMapper.userView(saved);
    }

    @Override
    @Transactional
    public Views.UserView createAdministrator(AdminCommands.CreateAdministrator command) {
        if (command.adminType() == null) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Le type d'administrateur (adminType) est obligatoire");
        }
        Email email = Email.of(command.email());
        PhoneNumber phone = command.phone() == null || command.phone().isBlank()
                ? null : PhoneNumber.of(command.phone());
        PasswordPolicy.validate(command.password());

        if (userRepository.existsByEmail(email.value())) {
            throw IamException.of(IamErrorCode.EMAIL_ALREADY_EXISTS, "Un compte existe deja avec cet email");
        }
        if (phone != null && userRepository.existsByPhone(phone.value())) {
            throw IamException.of(IamErrorCode.PHONE_ALREADY_EXISTS, "Un compte existe deja avec ce telephone");
        }

        String hashed = passwordHasher == null ? command.password() : passwordHasher.hash(command.password());
        User admin = User.register(email, phone, hashed, ProfileType.ADMIN, clock);
        admin.setAdminType(command.adminType());
        admin.markEmailVerified();
        if (phone != null) {
            admin.markPhoneVerified();
        }
        admin.activate(clock);

        String roleName = roleNameFor(command.adminType());
        if (roleRepository != null) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Role systeme introuvable : " + roleName));
            admin.addRole(role);
        }

        User saved = userRepository.save(admin);
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.ADMIN_CREATED,
                "administrateur " + command.adminType() + " cree (" + saved.getEmail() + ")");
        return viewMapper.userView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.UserView> listAdministrators(AdminType adminType) {
        return userRepository.findAll(0, 200).stream()
                .filter(u -> u.getProfileType() == ProfileType.ADMIN)
                .filter(u -> adminType == null || u.getAdminType() == adminType)
                .map(viewMapper::userView)
                .toList();
    }

    @Override
    @Transactional
    public Views.UserView updateDirectPermissions(AdminCommands.UpdateDirectPermissions command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        user.setDirectPermissions(command.permissionCodes() == null
                ? new HashSet<>() : new HashSet<>(command.permissionCodes()));
        user.setUpdatedAt(clock.instant());
        User saved = userRepository.save(user);
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.DIRECT_PERMISSIONS_UPDATED,
                "permissions directes de #" + saved.getId() + " -> " + saved.getDirectPermissions());
        return viewMapper.userView(saved);
    }

    @Override
    @Transactional
    public void revokeUserTokens(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        if (sessionRepository != null) {
            for (UserSession session : sessionRepository.findByUserId(userId)) {
                if (session.isActive()) {
                    session.revoke(clock);
                    sessionRepository.save(session);
                }
            }
        }
        if (refreshTokenRepository != null) {
            for (RefreshToken token : refreshTokenRepository.findByUserId(userId)) {
                if (token.isValid(clock)) {
                    token.revoke(clock);
                    refreshTokenRepository.save(token);
                }
            }
        }
        user.revokeAllTokens(clock);
        userRepository.save(user);
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.GLOBAL_TOKENS_REVOKED,
                "revocation instantanee de tous les jetons et sessions de l'utilisateur #" + userId);
    }

    private static String roleNameFor(AdminType adminType) {
        return switch (adminType) {
            case SUPER_ADMIN -> Role.SUPER_ADMIN;
            case MODERATOR -> Role.MODERATOR;
            case TECHNICAL_SUPPORT -> Role.TECHNICAL_SUPPORT;
            case COMMERCIAL_SUPPORT -> Role.COMMERCIAL_SUPPORT;
            case DATA_ANALYST -> Role.DATA_ANALYST;
        };
    }
}
