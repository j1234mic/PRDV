package com.prdv.rdv.profile.adapter.out.persistence.adapter;

import com.prdv.rdv.profile.adapter.out.persistence.entity.MedicalRecordEntity;
import com.prdv.rdv.profile.adapter.out.persistence.mapper.MedicalPersistenceMapper;
import com.prdv.rdv.profile.adapter.out.persistence.repository.MedicalRecordJpaRepository;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adapteur JPA du dossier medical personnel. */
@Repository
public class MedicalRecordPersistenceAdapter implements MedicalRecordRepository {

    private final MedicalRecordJpaRepository jpa;
    private final MedicalPersistenceMapper mapper;

    public MedicalRecordPersistenceAdapter(MedicalRecordJpaRepository jpa,
                                           MedicalPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public MedicalRecord save(MedicalRecord record) {
        MedicalRecordEntity existing = jpa.findByPatientUserId(record.getPatientUserId()).orElse(null);
        MedicalRecordEntity entity = mapper.toEntity(record);
        if (existing != null) {
            entity.setId(existing.getId());
        }
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<MedicalRecord> findByPatientUserId(Long patientUserId) {
        return jpa.findByPatientUserId(patientUserId).map(mapper::toDomain);
    }

    @Override
    public void deleteByPatientUserId(Long patientUserId) {
        jpa.deleteByPatientUserId(patientUserId);
    }
}
