package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.RoleEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.PermissionJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.RoleJpaRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapteur JPA des roles RBAC (roles personnalisables, permissions associees).
 */
@Repository
public class RolePersistenceAdapter implements RoleRepository {

    private final RoleJpaRepository roleJpa;
    private final PermissionJpaRepository permissionJpa;
    private final UserPersistenceMapper userMapper;

    public RolePersistenceAdapter(RoleJpaRepository roleJpa, PermissionJpaRepository permissionJpa,
                                  UserPersistenceMapper userMapper) {
        this.roleJpa = roleJpa;
        this.permissionJpa = permissionJpa;
        this.userMapper = userMapper;
    }

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
}
