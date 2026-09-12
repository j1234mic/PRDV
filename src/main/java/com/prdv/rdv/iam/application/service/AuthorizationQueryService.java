package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.AuthorizationQueryUseCase;
import com.prdv.rdv.iam.application.port.output.DelegationRepository;
import com.prdv.rdv.iam.application.port.output.PermissionRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

/**
 * Resolution des autorites effectives, recalculee a chaque requete :
 * roles + permissions directes + delegations temporaires actives.
 * Le super-administrateur recupere la totalite du catalogue de permissions.
 */
@Service
public class AuthorizationQueryService implements AuthorizationQueryUseCase {

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final DelegationRepository delegationRepository;
    private final Clock clock;

    public AuthorizationQueryService(UserRepository userRepository,
                                     PermissionRepository permissionRepository,
                                     DelegationRepository delegationRepository, Clock clock) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.delegationRepository = delegationRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> authoritiesFor(Long userId) {
        return userRepository.findById(userId).map(this::resolve).orElseGet(Set::of);
    }

    private Set<String> resolve(User user) {
        Set<String> authorities = new HashSet<>();
        boolean superAdmin = false;

        for (Role role : user.getRoles()) {
            authorities.add(role.getName());
            if (Role.SUPER_ADMIN.equals(role.getName())) {
                superAdmin = true;
            }
            for (Permission permission : role.getPermissions()) {
                authorities.add(permission.getCode());
            }
        }

        if (superAdmin) {
            permissionRepository.findAll().forEach(p -> authorities.add(p.getCode()));
        }

        authorities.addAll(user.getDirectPermissions());

        for (Delegation delegation : delegationRepository.findActiveByGrantee(user.getId(), clock.instant())) {
            authorities.addAll(delegation.getPermissionCodes());
        }
        return authorities;
    }
}
