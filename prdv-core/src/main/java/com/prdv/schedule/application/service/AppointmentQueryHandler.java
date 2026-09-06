package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.GetAppointmentsUseCase;
import com.prdv.schedule.application.port.out.AppointmentRepository;

import java.time.LocalDate;
import java.util.List;

public final class AppointmentQueryHandler implements GetAppointmentsUseCase {

    private final AppointmentRepository appointments;

    public AppointmentQueryHandler(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    @Override
    public List<Appointment> forPatient(Long patientUserId, int limit) {
        return appointments.findByPatient(patientUserId, Math.min(Math.max(1, limit), 100));
    }

    @Override
    public List<Appointment> forDoctorOn(Long doctorUserId, LocalDate day) {
        return appointments.findByDoctorBetween(doctorUserId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }
}
