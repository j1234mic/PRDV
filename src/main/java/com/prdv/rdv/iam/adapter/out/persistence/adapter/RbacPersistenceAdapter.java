package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.DelegationEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.RoleEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.DelegationJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.PermissionJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.RoleJpaRepository;
import com.prdv.rdv.iam.application.port.output.DelegationRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapteur JPA des roles et delegations.
 *
 * <p>Les permissions sont gerees par {@link PermissionPersistenceAdapter} :
 * {@code RoleRepository} et {@code PermissionRepository} exposent toutes deux
 * {@code findAll()} avec des types de retour differents.
 */
@Repository
public class RbacPersistenceAdapter implements RoleRepository, DelegationRepository {

    private final RoleJpaRepository roleJpa;
    private final PermissionJpaRepository permissionJpa;
    private final DelegationJpaRepository delegationJpa;
    private final UserPersistenceMapper userMapper;
    private final AuthPersistenceMapper authMapper;

    public RbacPersistenceAdapter(RoleJpaRepository roleJpa, PermissionJpaRepository permissionJpa,
                                  DelegationJpaRepository delegationJpa,
                                  UserPersistenceMapper userMapper, AuthPersistenceMapper authMapper) {
        this.roleJpa = roleJpa;
        this.permissionJpa = permissionJpa;
        this.delegationJpa = delegationJpa;
        this.userMapper = userMapper;
        this.authMapper = authMapper;
    }

    // --------------------------------------------------------------- Role
    @Override
    public Role save(Role role) {
        RoleEntity entity = roleJpa.findByName(role.getName()).orElseGet(RoleEntity::new);
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        entity.setSystemRole(role.isSystem());
        Set<String> codes = new HashSet<>();
        role.getPermissions().forEach(p -> codes.add(p.getCode()));
        entity.setPermissions(new HashSet<>(permissionJpa.findByCodeIn(codes)));
        return userMapper.toRoleDomain(roleJpa.save(entity));
    }

    @Override
    public Optional<Role> findByName(String name) {
        return roleJpa.findByName(name).map(userMapper::toRoleDomain);
    }

    @Override
    public List<Role> findAll() {
        return roleJpa.findAll().stream().map(userMapper::toRoleDomain).toList();
    }

    @Override
    public Set<Role> findByNames(Collection<String> names) {
        return new HashSet<>(roleJpa.findByNameIn(names).stream().map(userMapper::toRoleDomain).toList());
    }

    // --------------------------------------------------------- Delegation
    @Override
    public Delegation save(Delegation delegation) {
        DelegationEntity entity = delegation.getId() == null
                ? new DelegationEntity()
                : delegationJpa.findById(delegation.getId()).orElseGet(DelegationEntity::new);
        DelegationEntity mapped = authMapper.toEntity(delegation);
        mapped.setId(entity.getId());
        return authMapper.toDomain(delegationJpa.save(mapped));
    }

    @Override
    public Optional<Delegation> findById(Long id) {
        return delegationJpa.findById(id).map(authMapper::toDomain);
    }

    @Override
    public List<Delegation> findActiveByGrantee(Long granteeUserId, Instant now) {
        return delegationJpa.findActiveByGrantee(granteeUserId, now).stream()
                .map(authMapper::toDomain).toList();
    }

    @Override
    public List<Delegation> findByGranter(Long granterUserId) {
        return delegationJpa.findByGranterUserId(granterUserId).stream()
                .map(authMapper::toDomain).toList();
    }
}
