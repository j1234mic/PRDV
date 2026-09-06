package com.prdv.profile.application.service;

import com.prdv.profile.application.model.DoctorSummary;
import com.prdv.profile.application.port.in.DoctorDirectoryUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;

import java.util.List;

/** Lecture seule : l'annuaire expose les profils VERIFIES uniquement. */
public final class DoctorDirectoryHandler implements DoctorDirectoryUseCase {

    private final DoctorProfileRepository doctors;

    public DoctorDirectoryHandler(DoctorProfileRepository doctors) {
        this.doctors = doctors;
    }

    @Override
    public List<DoctorSummary> search(String specialty, int limit) {
        int capped = Math.min(Math.max(limit, 1), 50);
        return doctors.findVerifiedBySpecialty(specialty == null ? "" : specialty, capped).stream()
                .map(d -> new DoctorSummary(d.userId(), d.fullName(), d.specialty(), d.sector(),
                        d.consultationFeeCents(),
                        d.practiceLocations().isEmpty() ? null : d.practiceLocations().get(0).city(),
                        null))
                .toList();
    }
}
