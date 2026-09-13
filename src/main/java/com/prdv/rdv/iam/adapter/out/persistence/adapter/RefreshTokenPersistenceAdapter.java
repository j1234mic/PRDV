package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.RefreshTokenEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.RefreshTokenJpaRepository;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.domain.model.auth.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des jetons de rafraichissement (rotation par famille).
 */
@Repository
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshJpa;
    private final AuthPersistenceMapper mapper;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository refreshJpa,
                                          AuthPersistenceMapper mapper) {
        this.refreshJpa = refreshJpa;
        this.mapper = mapper;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity entity = token.getId() == null
                ? new RefreshTokenEntity()
                : refreshJpa.findById(token.getId()).orElseGet(RefreshTokenEntity::new);
        RefreshTokenEntity mapped = mapper.toEntity(token);
        mapped.setId(entity.getId());
        return mapper.toDomain(refreshJpa.save(mapped));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshJpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllByFamily(String family) {
        return refreshJpa.findAllByFamily(family).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<RefreshToken> findByUserId(Long userId) {
        return refreshJpa.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }
}
