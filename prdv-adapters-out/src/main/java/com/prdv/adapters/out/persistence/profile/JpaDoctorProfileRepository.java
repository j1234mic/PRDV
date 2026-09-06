package com.prdv.adapters.out.persistence.profile;

import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.profile.domain.model.DoctorProfile;
import com.prdv.profile.domain.model.DoctorVerificationStatus;
import com.prdv.profile.domain.model.PracticeLocation;
import com.prdv.profile.domain.model.RppsNumber;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class JpaDoctorProfileRepository implements DoctorProfileRepository {

    private final DoctorProfileJpaRepository jpa;

    public JpaDoctorProfileRepository(DoctorProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public DoctorProfile save(DoctorProfile profile) {
        DoctorProfileEntity e = new DoctorProfileEntity();
        e.setId(profile.id());
        e.setUserId(profile.userId());
        e.setFullName(profile.fullName());
        e.setRpps(profile.rpps().value());
        e.setSpecialty(profile.specialty());
        e.setSubSpecialties(profile.subSpecialties());
        e.setDescription(profile.description());
        e.setSector(profile.sector());
        e.setConsultationFeeCents(profile.consultationFeeCents());
        e.setLanguages(profile.languages());
        e.setLocations(profile.practiceLocations());
        e.setVerificationStatus(profile.verification());
        e.setRejectionReason(profile.rejectionReason());
        DoctorProfileEntity saved = jpa.save(e);
        if (profile.id() == null) {
            profile.assignId(saved.getId());
        }
        return profile;
    }

    @Override
    public Optional<DoctorProfile> findByUserId(Long userId) {
        return jpa.findByUserId(userId).map(JpaDoctorProfileRepository::toDomain);
    }

    @Override
    public Optional<DoctorProfile> findByRpps(String rpps) {
        return jpa.findByRpps(rpps).map(JpaDoctorProfileRepository::toDomain);
    }

    @Override
    public List<DoctorProfile> findVerifiedBySpecialty(String specialty, int limit) {
        return jpa.searchVerified(DoctorVerificationStatus.VERIFIED, specialty == null ? "" : specialty, PageRequest.of(0, limit)).stream()
                .map(JpaDoctorProfileRepository::toDomain)
                .toList();
    }

    @Override
    public List<DoctorProfile> findAllByVerificationStatus(String status, int limit) {
        return jpa.findByVerificationStatusOrderByFullNameAsc(DoctorVerificationStatus.valueOf(status),
                        PageRequest.of(0, limit)).stream()
                .map(JpaDoctorProfileRepository::toDomain)
                .toList();
    }

    private static DoctorProfile toDomain(DoctorProfileEntity e) {
        return new DoctorProfile(e.getId(), e.getUserId(), e.getFullName(), new RppsNumber(e.getRpps()),
                e.getSpecialty(), e.getSubSpecialties(), e.getDescription(), e.getSector(),
                e.getConsultationFeeCents(), e.getLanguages(), e.getLocations(),
                e.getVerificationStatus(), e.getRejectionReason());
    }
}
