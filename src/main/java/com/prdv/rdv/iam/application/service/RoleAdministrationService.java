package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.RoleAdministrationUseCase;
import com.prdv.rdv.iam.application.port.output.PermissionRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Administration des roles et permissions (RBAC) et attribution aux comptes.
 */
@Service
public class RoleAdministrationService implements RoleAdministrationUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final SecurityContextPort securityContext;
    private final ViewMapper viewMapper;
    private final AuditLogger auditLogger;

    public RoleAdministrationService(RoleRepository roleRepository,
                                     PermissionRepository permissionRepository,
                                     UserRepository userRepository,
                                     SecurityContextPort securityContext,
                                     ViewMapper viewMapper, AuditLogger auditLogger) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
        this.securityContext = securityContext;
        this.viewMapper = viewMapper;
        this.auditLogger = auditLogger;
    }

    @Override
    @Transactional
    public Views.RoleView createRole(AdminCommands.CreateRole command) {
        String name = normalizeRoleName(command.name());
        if (roleRepository.findByName(name).isPresent()) {
            throw IamException.of(IamErrorCode.EMAIL_ALREADY_EXISTS,
                    "Un role avec ce nom existe deja");
        }
        Set<Permission> permissions = command.permissionCodes() == null
                ? new HashSet<>() : new HashSet<>(permissionRepository.findByCodes(command.permissionCodes()));
        Role role = roleRepository.save(Role.custom(name, command.description(), permissions));
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.ROLE_CREATED, "creation du role " + name);
        return viewMapper.roleView(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.RoleView> listRoles() {
        return roleRepository.findAll().stream().map(viewMapper::roleView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.PermissionView> listPermissions() {
        return permissionRepository.findAll().stream().map(viewMapper::permissionView).toList();
    }

    @Override
    @Transactional
    public void assignRoles(AdminCommands.AssignRoles command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> IamException.of(IamErrorCode.NOT_FOUND, "Utilisateur introuvable"));
        Set<Role> roles = roleRepository.findByNames(command.roleNames());
        if (roles.size() != command.roleNames().size()) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Un ou plusieurs roles sont introuvables");
        }
        user.setRoles(new HashSet<>(roles));
        userRepository.save(user);
        auditLogger.success(securityContext.currentUserId().orElse(null),
                AuditLog.Action.ROLE_ASSIGNED,
                "roles de l'utilisateur #" + user.getId() + " -> " + command.roleNames());
    }

    private static String normalizeRoleName(String name) {
        String normalized = name == null ? "" : name.trim().toUpperCase().replace(' ', '_');
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        if (!normalized.matches("^ROLE_[A-Z0-9_]+$")) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Nom de role invalide (lettres majuscules, chiffres, underscores)");
        }
        return normalized;
    }
}
