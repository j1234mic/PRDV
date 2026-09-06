package com.prdv.profile.application.service;

import com.prdv.profile.application.port.in.ValidateDoctorProfileUseCase;
import com.prdv.profile.application.port.out.DoctorProfileRepository;
import com.prdv.profile.domain.event.DoctorProfileValidatedEvent;
import com.prdv.profile.domain.model.DoctorProfile;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.shared.exception.NotFoundException;

/** Validation manuelle par un ADMIN (module 1.1). Publie l'evenement -> notification au medecin (Observer). */
public final class DoctorValidationHandler implements ValidateDoctorProfileUseCase {

    private final DoctorProfileRepository doctors;
    private final DomainEventPublisher events;

    public DoctorValidationHandler(DoctorProfileRepository doctors, DomainEventPublisher events) {
        this.doctors = doctors;
        this.events = events;
    }

    @Override
    public void approve(Long doctorUserId) {
        DoctorProfile profile = load(doctorUserId);
        profile.approve();
        doctors.save(profile);
        events.publish(new DoctorProfileValidatedEvent(doctorUserId, true, null));
    }

    @Override
    public void reject(Long doctorUserId, String reason) {
        DoctorProfile profile = load(doctorUserId);
        profile.reject(reason);
        doctors.save(profile);
        events.publish(new DoctorProfileValidatedEvent(doctorUserId, false, reason));
    }

    private DoctorProfile load(Long doctorUserId) {
        return doctors.findByUserId(doctorUserId)
                .orElseThrow(() -> new NotFoundException("Profil praticien introuvable"));
    }
}
