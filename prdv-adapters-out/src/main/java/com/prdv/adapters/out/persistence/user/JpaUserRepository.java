package com.prdv.adapters.out.persistence.user;

import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.model.PasswordHash;
import com.prdv.identity.domain.model.User;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** ADAPTATEUR (pattern ADAPTER + IMPL du port REPOSITORY) : convertit modele JPA <-> domaine. */
@Component
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpa;

    public JpaUserRepository(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPhone(user.phone());
        entity.setPasswordHash(user.passwordHash().value());
        entity.setRole(user.role());
        entity.setStatus(user.status());
        entity.setEmailVerified(user.emailVerified());
        entity.setFailedAttempts(user.failedAttempts());
        entity.setLockedUntil(user.lockedUntil());
        entity.setCreatedAt(user.createdAt());
        UserEntity saved = jpa.save(entity);
        if (user.id() == null) {
            user.assignId(saved.getId());
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(JpaUserRepository::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmailIgnoreCase(email).map(JpaUserRepository::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmailIgnoreCase(email);
    }

    private static User toDomain(UserEntity e) {
        return User.restore(e.getId(), e.getEmail(), e.getPhone(), new PasswordHash(e.getPasswordHash()),
                e.getRole(), e.getStatus(), e.isEmailVerified(), e.getFailedAttempts(),
                e.getLockedUntil(), e.getCreatedAt());
    }
}
