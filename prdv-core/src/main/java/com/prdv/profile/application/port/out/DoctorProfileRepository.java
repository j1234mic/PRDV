package com.prdv.profile.application.port.out;

import com.prdv.profile.domain.model.DoctorProfile;

import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository {
    DoctorProfile save(DoctorProfile profile);
    Optional<DoctorProfile> findByUserId(Long userId);
    Optional<DoctorProfile> findByRpps(String rpps);
    List<DoctorProfile> findVerifiedBySpecialty(String specialty, int limit);
    List<DoctorProfile> findAllByVerificationStatus(String status, int limit);
}
