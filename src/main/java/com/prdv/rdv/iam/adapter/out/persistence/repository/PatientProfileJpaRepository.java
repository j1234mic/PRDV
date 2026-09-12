package com.prdv.rdv.iam.adapter.out.persistence.repository;

import com.prdv.rdv.iam.adapter.out.persistence.entity.PatientProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientProfileJpaRepository extends JpaRepository<PatientProfileEntity, Long> {

    Optional<PatientProfileEntity> findByUserId(Long userId);
}
