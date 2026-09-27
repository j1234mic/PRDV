package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.PatientIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientIdentityJpaRepository extends JpaRepository<PatientIdentityEntity, Long> {

    Optional<PatientIdentityEntity> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
