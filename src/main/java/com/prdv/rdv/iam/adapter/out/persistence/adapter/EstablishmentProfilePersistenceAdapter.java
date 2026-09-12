package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.EstablishmentProfileJpaRepository;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EstablishmentProfilePersistenceAdapter implements EstablishmentProfileRepository {

    private final EstablishmentProfileJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    public EstablishmentProfilePersistenceAdapter(EstablishmentProfileJpaRepository jpa,
                                                  UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public EstablishmentProfile save(EstablishmentProfile profile) {
        EstablishmentProfileEntity existing = jpa.findByUserId(profile.getUserId()).orElse(null);
        EstablishmentProfileEntity entity = mapper.toEntity(profile);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<EstablishmentProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<EstablishmentProfile> findPendingValidation() {
        return jpa.findPendingValidation(AccountStatus.PENDING_VALIDATION).stream()
                .map(mapper::toDomain).toList();
    }
}
