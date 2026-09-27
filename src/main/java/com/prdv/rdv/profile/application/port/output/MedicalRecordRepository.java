package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;

import java.util.Optional;

/** Persistance du dossier medical personnel. */
public interface MedicalRecordRepository {

    MedicalRecord save(MedicalRecord record);

    Optional<MedicalRecord> findByPatientUserId(Long patientUserId);

    void deleteByPatientUserId(Long patientUserId);
}
