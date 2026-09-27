package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PrivacyPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivacyPreferencesJpaRepository extends JpaRepository<PrivacyPreferencesEntity, Long> {

    Optional<PrivacyPreferencesEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
