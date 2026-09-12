package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.PatientProfile;

import java.util.Optional;

public interface PatientProfileRepository {

    PatientProfile save(PatientProfile profile);

    Optional<PatientProfile> findByUserId(Long userId);
}
