package com.prdv.profile.application.port.out;

import com.prdv.profile.domain.model.PatientProfile;

import java.util.Optional;

public interface PatientProfileRepository {
    PatientProfile save(PatientProfile profile);
    Optional<PatientProfile> findByUserId(Long userId);
}
