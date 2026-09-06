package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.Appointment;

import java.time.LocalDateTime;

public interface BookAppointmentUseCase {

    record Command(Long patientUserId, Long doctorUserId, LocalDateTime start, String type, String reason) { }

    /**
     * Reserve un creneau et retire automatiquement le patient de la liste d'attente
     * de ce medecin (module 4.3 "Attribution automatique").
     */
    Appointment book(Command command);
}
