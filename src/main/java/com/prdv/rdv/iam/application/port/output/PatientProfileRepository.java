package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.user.PatientProfile;

import java.util.List;
import java.util.Optional;

public interface PatientProfileRepository {

    PatientProfile save(PatientProfile profile);

    Optional<PatientProfile> findByUserId(Long userId);

    default List<PatientProfile> findByGuardianUserId(Long guardianUserId) {
        return List.of();
    }
}
