package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.Appointment;

import java.time.LocalDate;
import java.util.List;

/** Lecture (pattern CQRS-lite) : les requetes ont leur propre port, les handlers restent minces. */
public interface GetAppointmentsUseCase {

    List<Appointment> forPatient(Long patientUserId, int limit);

    List<Appointment> forDoctorOn(Long doctorUserId, LocalDate day);
}
