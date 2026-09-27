package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PatientIdentityEntity;
import com.prdv.rdv.profile.adapter.out.persistence.mapper.IdentityPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.PatientIdentityJpaRepository;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adapteur JPA de l'identite patient. */
@Repository
public class PatientIdentityPersistenceAdapter implements PatientIdentityRepository {

    private final PatientIdentityJpaRepository jpa;
    private final IdentityPersistenceMapper mapper;

    public PatientIdentityPersistenceAdapter(PatientIdentityJpaRepository jpa,
                                             IdentityPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PatientIdentity save(PatientIdentity identity) {
        PatientIdentityEntity existing = jpa.findByUserId(identity.getUserId()).orElse(null);
        PatientIdentityEntity entity = mapper.toEntity(identity);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PatientIdentity> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}
