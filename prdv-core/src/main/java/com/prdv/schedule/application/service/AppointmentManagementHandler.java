package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.ManageAppointmentUseCase;
import com.prdv.schedule.application.port.out.AppointmentRepository;
import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.domain.event.AppointmentEvents;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.shared.exception.ForbiddenOperationException;
import com.prdv.shared.exception.NotFoundException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Modification / annulation du RDV (module 4.2 "Gestion du RDV").
 * Autorisation : le domaine ne verifie pas le role HTTP mais l'IDENTITE fonctionnelle
 * (patient du RDV ou medecin du RDV) -> securite au coeur, pas seulement a la peripherie.
 */
public final class AppointmentManagementHandler implements ManageAppointmentUseCase {

    private final AppointmentRepository appointments;
    private final DoctorScheduleRepository schedules;
    private final DomainEventPublisher events;
    private final Clock clock;

    public AppointmentManagementHandler(AppointmentRepository appointments, DoctorScheduleRepository schedules,
                                        DomainEventPublisher events, Clock clock) {
        this.appointments = appointments;
        this.schedules = schedules;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public int cancelByPatient(Long appointmentId, Long requestingPatientUserId, String reason) {
        Appointment appointment = load(appointmentId);
        requirePatient(appointment, requestingPatientUserId);
        DoctorSchedule schedule = scheduleOf(appointment);
        LocalDateTime now = LocalDateTime.now(clock);

        appointment.cancelByPatient(schedule.cancellationPolicy(), reason, now);
        appointments.save(appointment);
        events.publish(new AppointmentEvents.Cancelled(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), appointment.slot().start(), appointment.slot().end(),
                "PATIENT", appointment.cancellationFeeCents()));
        return appointment.cancellationFeeCents();
    }

    @Override
    public void cancelByDoctor(Long appointmentId, Long requestingDoctorUserId, String reason) {
        Appointment appointment = load(appointmentId);
        requireDoctor(appointment, requestingDoctorUserId);
        appointment.cancelByDoctor(reason, LocalDateTime.now(clock));
        appointments.save(appointment);
        events.publish(new AppointmentEvents.Cancelled(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), appointment.slot().start(), appointment.slot().end(),
                "DOCTOR", 0));
    }

    @Override
    public Appointment rescheduleByPatient(Long appointmentId, Long requestingPatientUserId, LocalDateTime newStart) {
        Appointment appointment = load(appointmentId);
        requirePatient(appointment, requestingPatientUserId);
        DoctorSchedule schedule = scheduleOf(appointment);
        LocalDateTime now = LocalDateTime.now(clock);
        Slot newSlot = new Slot(newStart, appointment.slot().durationMinutes());

        if (!schedule.isBookable(newSlot, now)) {
            throw new NotFoundException("Nouveau creneau indisponible pour l'agenda du praticien");
        }
        appointments.lockDoctorSchedule(appointment.doctorUserId());
        if (!appointments.findActiveOverlapping(appointment.doctorUserId(), newSlot.start(), newSlot.end()).isEmpty()) {
            throw new NotFoundException("Nouveau creneau deja pris");
        }
        LocalDateTime oldStart = appointment.slot().start();
        appointment.rescheduleTo(newSlot, schedule.cancellationPolicy(), now);
        appointments.save(appointment);
        events.publish(new AppointmentEvents.Rescheduled(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), oldStart, newStart));
        return appointment;
    }

    @Override
    public void confirm(Long appointmentId, Long requestingDoctorUserId) {
        Appointment appointment = load(appointmentId);
        requireDoctor(appointment, requestingDoctorUserId);
        appointment.confirm(LocalDateTime.now(clock));
        appointments.save(appointment);
        events.publish(new AppointmentEvents.ConfirmedByDoctor(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), appointment.slot().start()));
    }

    @Override
    public void markNoShow(Long appointmentId, Long requestingDoctorUserId) {
        Appointment appointment = load(appointmentId);
        requireDoctor(appointment, requestingDoctorUserId);
        appointment.markNoShow(LocalDateTime.now(clock));
        appointments.save(appointment);
    }

    // ----- helpers ------------------------------------------------------------

    private Appointment load(Long id) {
        return appointments.findById(id).orElseThrow(() -> new NotFoundException("Rendez-vous introuvable"));
    }

    private DoctorSchedule scheduleOf(Appointment appointment) {
        return schedules.findByDoctorUserId(appointment.doctorUserId())
                .orElseGet(() -> DoctorSchedule.create(appointment.doctorUserId()));
    }

    private static void requirePatient(Appointment appointment, Long userId) {
        if (!appointment.patientUserId().equals(userId)) {
            throw new ForbiddenOperationException("Seul le patient concerne peut agir sur ce rendez-vous");
        }
    }

    private static void requireDoctor(Appointment appointment, Long userId) {
        if (!appointment.doctorUserId().equals(userId)) {
            throw new ForbiddenOperationException("Seul le praticien concerne peut agir sur ce rendez-vous");
        }
    }
}
