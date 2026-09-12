package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.PractitionerProfileJpaRepository;
import com.prdv.rdv.iam.application.port.output.PractitionerProfileRepository;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PractitionerProfilePersistenceAdapter implements PractitionerProfileRepository {

    private final PractitionerProfileJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    public PractitionerProfilePersistenceAdapter(PractitionerProfileJpaRepository jpa,
                                                 UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PractitionerProfile save(PractitionerProfile profile) {
        PractitionerProfileEntity existing = jpa.findByUserId(profile.getUserId()).orElse(null);
        PractitionerProfileEntity entity = mapper.toEntity(profile);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PractitionerProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<PractitionerProfile> findPendingValidation() {
        return jpa.findPendingValidation(AccountStatus.PENDING_VALIDATION).stream()
                .map(mapper::toDomain).toList();
    }
}
