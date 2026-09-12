package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.UserEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.RoleJpaRepository;
import com.prdv.rdv.iam.adapter.out.persistence.repository.UserJpaRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapteur JPA de l'agregat {@link User}. Les profils specialises ont leurs
 * propres adapteurs (les ports partagent le nom findByUserId avec des types
 * de retour differents, d'ou des classes separees).
 */
@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpa;
    private final RoleJpaRepository roleJpa;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(UserJpaRepository userJpa, RoleJpaRepository roleJpa,
                                  UserPersistenceMapper mapper) {
        this.userJpa = userJpa;
        this.roleJpa = roleJpa;
        this.mapper = mapper;
    }

    @Override
    public User save(User domain) {
        UserEntity entity = domain.getId() == null
                ? new UserEntity()
                : userJpa.findById(domain.getId()).orElseGet(UserEntity::new);
        mapper.copyToEntity(domain, entity);

        // On ne rattache que des roles persistes : resolution par nom
        Set<String> roleNames = new HashSet<>();
        for (Role role : domain.getRoles()) {
            roleNames.add(role.getName());
        }
        entity.setRoles(new HashSet<>(roleJpa.findByNameIn(roleNames)));

        return mapper.toDomain(userJpa.save(entity));
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpa.findByEmailIgnoreCase(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userJpa.findByPhone(phone).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpa.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return phone != null && userJpa.existsByPhone(phone);
    }

    @Override
    public List<User> findAll(int page, int size) {
        return userJpa.findAll(PageRequest.of(page, size)).map(mapper::toDomain).getContent();
    }

    @Override
    public long count() {
        return userJpa.count();
    }
}
