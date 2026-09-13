package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.DelegationEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.AuthPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.DelegationJpaRepository;
import com.prdv.rdv.iam.application.port.output.DelegationRepository;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapteur JPA des delegations temporaires de droits (bornees dans le temps,
 * revoquables instantanement).
 */
@Repository
public class DelegationPersistenceAdapter implements DelegationRepository {

    private final DelegationJpaRepository delegationJpa;
    private final AuthPersistenceMapper authMapper;

    public DelegationPersistenceAdapter(DelegationJpaRepository delegationJpa,
                                        AuthPersistenceMapper authMapper) {
        this.delegationJpa = delegationJpa;
        this.authMapper = authMapper;
    }

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
