package com.prdv.rdv.iam.adapter.out.persistence.adapter;

import com.prdv.rdv.iam.adapter.out.persistence.entity.SecretaryProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.prdv.rdv.iam.adapter.out.persistence.repository.SecretaryProfileJpaRepository;
import com.prdv.rdv.iam.application.port.output.SecretaryProfileRepository;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SecretaryProfilePersistenceAdapter implements SecretaryProfileRepository {

    private final SecretaryProfileJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    public SecretaryProfilePersistenceAdapter(SecretaryProfileJpaRepository jpa, UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public SecretaryProfile save(SecretaryProfile profile) {
        SecretaryProfileEntity existing = jpa.findByUserId(profile.getUserId()).orElse(null);
        SecretaryProfileEntity entity = mapper.toEntity(profile);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<SecretaryProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public List<SecretaryProfile> findBySupervisedPractitionerId(Long practitionerUserId) {
        return jpa.findBySupervisedPractitionerId(practitionerUserId).stream()
                .map(mapper::toDomain).toList();
    }
}
