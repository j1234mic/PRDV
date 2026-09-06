package com.prdv.profile.application.port.in;

/** Decision du moderation board (ADMIN uniquement, autorite verifiee a la porte d'entree REST). */
public interface ValidateDoctorProfileUseCase {
    void approve(Long doctorUserId);
    void reject(Long doctorUserId, String reason);
}
