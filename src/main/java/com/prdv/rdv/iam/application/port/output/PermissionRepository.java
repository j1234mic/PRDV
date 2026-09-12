package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.rbac.Permission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PermissionRepository {

    List<Permission> saveAll(Collection<Permission> permissions);

    Optional<Permission> findByCode(String code);

    List<Permission> findAll();

    Set<Permission> findByCodes(Collection<String> codes);
}
