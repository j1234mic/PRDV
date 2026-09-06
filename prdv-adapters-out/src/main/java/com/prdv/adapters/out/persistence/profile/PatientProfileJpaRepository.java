package com.prdv.adapters.out.persistence.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface PatientProfileJpaRepository extends JpaRepository<PatientProfileEntity, Long> {
    Optional<PatientProfileEntity> findByUserId(Long userId);
}
