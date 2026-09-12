package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PatientProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.PatientProfileJpaRepository;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PatientProfilePersistenceAdapter implements PatientProfileRepository {

    private final PatientProfileJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    public PatientProfilePersistenceAdapter(PatientProfileJpaRepository jpa, UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PatientProfile save(PatientProfile profile) {
        PatientProfileEntity existing = jpa.findByUserId(profile.getUserId()).orElse(null);
        PatientProfileEntity entity = mapper.toEntity(profile);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PatientProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }
}
