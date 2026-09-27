package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PrivacyPreferencesEntity;
import com.prdv.rdv.profile.adapter.out.persistence.mapper.IdentityPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.PrivacyPreferencesJpaRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adapteur JPA des preferences et consentements. */
@Repository
public class PrivacyPreferencesPersistenceAdapter implements PrivacyPreferencesRepository {

    private final PrivacyPreferencesJpaRepository jpa;
    private final IdentityPersistenceMapper mapper;

    public PrivacyPreferencesPersistenceAdapter(PrivacyPreferencesJpaRepository jpa,
                                                IdentityPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public PrivacyPreferences save(PrivacyPreferences preferences) {
        PrivacyPreferencesEntity existing = jpa.findByUserId(preferences.getUserId()).orElse(null);
        PrivacyPreferencesEntity entity = mapper.toEntity(preferences);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<PrivacyPreferences> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}
