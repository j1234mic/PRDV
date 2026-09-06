package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.Appointment;

import java.time.LocalDateTime;

public interface ManageAppointmentUseCase {

    /** Retourne le montant de penalite eventuel en centimes (facture module 7, a encaisser via PaymentGateway). */
    int cancelByPatient(Long appointmentId, Long requestingPatientUserId, String reason);

    void cancelByDoctor(Long appointmentId, Long requestingDoctorUserId, String reason);

    Appointment rescheduleByPatient(Long appointmentId, Long requestingPatientUserId, LocalDateTime newStart);

    void confirm(Long appointmentId, Long requestingDoctorUserId);

    void markNoShow(Long appointmentId, Long requestingDoctorUserId);
}
