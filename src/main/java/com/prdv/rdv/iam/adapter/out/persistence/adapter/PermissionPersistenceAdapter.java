package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PermissionEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.PermissionJpaRepository;
import com.prdv.rdv.iam.application.port.output.PermissionRepository;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapteur JPA des permissions granulaires {@code module:action}.
 */
@Repository
public class PermissionPersistenceAdapter implements PermissionRepository {

    private final PermissionJpaRepository permissionJpa;
    private final UserPersistenceMapper userMapper;

    public PermissionPersistenceAdapter(PermissionJpaRepository permissionJpa,
                                        UserPersistenceMapper userMapper) {
        this.permissionJpa = permissionJpa;
        this.userMapper = userMapper;
    }

    @Override
    public List<Permission> saveAll(Collection<Permission> permissions) {
        return permissions.stream().map(p -> {
            PermissionEntity entity = permissionJpa.findByCode(p.getCode()).orElseGet(PermissionEntity::new);
            entity.setCode(p.getCode());
            entity.setModule(p.getModule());
            entity.setDescription(p.getDescription());
            return userMapper.toPermissionDomain(permissionJpa.save(entity));
        }).toList();
    }

    @Override
    public Optional<Permission> findByCode(String code) {
        return permissionJpa.findByCode(code).map(userMapper::toPermissionDomain);
    }

    @Override
    public List<Permission> findAll() {
        return permissionJpa.findAll().stream().map(userMapper::toPermissionDomain).toList();
    }

    @Override
    public Set<Permission> findByCodes(Collection<String> codes) {
        return new HashSet<>(permissionJpa.findByCodeIn(codes).stream()
                .map(userMapper::toPermissionDomain).toList());
    }
}
