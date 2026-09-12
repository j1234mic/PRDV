package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.rbac.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    Set<Role> findByNames(Collection<String> names);
}
