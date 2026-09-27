package com.prdv.rdv.profile.adapter.out.persistence.repository;

import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalRecordJpaRepository extends JpaRepository<MedicalRecordEntity, Long> {

    Optional<MedicalRecordEntity> findByPatientUserId(Long patientUserId);

    void deleteByPatientUserId(Long patientUserId);
}
