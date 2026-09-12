package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.UserSessionEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.UserSessionJpaRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des sessions de connexion.
 *
 * <p>Separe de {@link AuthPersistenceAdapter} car {@code UserSessionRepository}
 * et {@code RefreshTokenRepository} declarent toutes deux {@code findByUserId(Long)}
 * avec des types de retour incompatibles.
 */
@Repository
public class UserSessionPersistenceAdapter implements UserSessionRepository {

    private final UserSessionJpaRepository sessionJpa;
    private final AuthPersistenceMapper mapper;

    public UserSessionPersistenceAdapter(UserSessionJpaRepository sessionJpa, AuthPersistenceMapper mapper) {
        this.sessionJpa = sessionJpa;
        this.mapper = mapper;
    }

    @Override
    public UserSession save(UserSession session) {
        UserSessionEntity entity = session.getId() == null
                ? new UserSessionEntity()
                : sessionJpa.findById(session.getId()).orElseGet(UserSessionEntity::new);
        UserSessionEntity mapped = mapper.toEntity(session);
        mapped.setId(entity.getId());
        return mapper.toDomain(sessionJpa.save(mapped));
    }

    @Override
    public Optional<UserSession> findById(Long id) {
        return sessionJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<UserSession> findByUserId(Long userId) {
        return sessionJpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<UserSession> findLatestActiveByUserId(Long userId) {
        return sessionJpa.findTopByUserIdAndRevokedAtIsNullOrderByLastSeenAtDesc(userId)
                .map(mapper::toDomain);
    }
}
