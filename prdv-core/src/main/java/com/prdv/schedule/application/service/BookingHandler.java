package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.BookAppointmentUseCase;
import com.prdv.schedule.application.port.out.AppointmentRepository;
import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.application.port.out.WaitingListRepository;
import com.prdv.schedule.domain.event.AppointmentEvents;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.AppointmentType;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.NotFoundException;
import com.prdv.shared.exception.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Prise de rendez-vous (module 4.2 etape 5).
 *
 * CONCURRENCE : lockDoctorSchedule() (SELECT FOR UPDATE dans l'adaptateur) serialise
 * les reservations sur le meme agenda, puis on re-verifie le chevauchement ; le couple
 * (verrou + relecture) rend le double-booking impossible meme sous race condition.
 * EVENT-DRIVEN : le succes publie Booked -> notifications + liste d'attente (Observer).
 */
public final class BookingHandler implements BookAppointmentUseCase {

    private final DoctorScheduleRepository schedules;
    private final AppointmentRepository appointments;
    private final WaitingListRepository waitingList;
    private final com.prdv.profile.application.port.out.DoctorProfileRepository doctors;
    private final com.prdv.profile.application.port.out.PatientProfileRepository patients;
    private final DomainEventPublisher events;
    private final Clock clock;

    public BookingHandler(DoctorScheduleRepository schedules, AppointmentRepository appointments,
                          WaitingListRepository waitingList,
                          com.prdv.profile.application.port.out.DoctorProfileRepository doctors,
                          com.prdv.profile.application.port.out.PatientProfileRepository patients,
                          DomainEventPublisher events, Clock clock) {
        this.schedules = schedules;
        this.appointments = appointments;
        this.waitingList = waitingList;
        this.doctors = doctors;
        this.patients = patients;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Appointment book(Command command) {
        // Invariants transverses : patient complet (dossier requis) et praticien VERIFIE.
        patients.findByUserId(command.patientUserId())
                .orElseThrow(() -> new ValidationException("Completez votre profil patient avant de reserver"));
        doctors.findByUserId(command.doctorUserId())
                .orElseThrow(() -> new NotFoundException("Profil praticien introuvable"))
                .ensureVerified();

        DoctorSchedule schedule = schedules.findByDoctorUserId(command.doctorUserId())
                .orElseThrow(() -> new NotFoundException("Agenda du praticien introuvable"));
        AppointmentType type = parseType(command.type());
        // La duree vient de la GRILLE du praticien (source de verite, cf. module 4.1).
        Slot slot = new Slot(command.start(), defaultDuration(schedule, command.start()));

        if (!schedule.isBookable(slot, LocalDateTime.now(clock))) {
            throw new ConflictException("Creneau hors horaires, trop proche ou bloque");
        }

        appointments.lockDoctorSchedule(command.doctorUserId());
        if (!appointments.findActiveOverlapping(command.doctorUserId(), slot.start(), slot.end()).isEmpty()) {
            throw new ConflictException("Ce creneau vient d'etre reserve");
        }

        Appointment appointment = Appointment.request(command.patientUserId(), command.doctorUserId(), slot,
                type, command.reason(), !schedule.requiresManualConfirmation(), "PATIENT",
                LocalDateTime.now(clock));
        if (type == AppointmentType.TELECONSULTATION) {
            appointment.attachTeleconsultationUrl(appointment.joinUrl(), LocalDateTime.now(clock));
        }
        appointment = appointments.save(appointment);

        waitingList.deleteByPatientAndDoctor(command.patientUserId(), command.doctorUserId());

        events.publish(new AppointmentEvents.Booked(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), slot.start(), slot.durationMinutes(), type.name(),
                appointment.status() == com.prdv.schedule.domain.model.AppointmentStatus.CONFIRMED, "PATIENT"));
        return appointment;
    }

    private static AppointmentType parseType(String raw) {
        try {
            return AppointmentType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Type de consultation inconnu : " + raw);
        }
    }

    private int defaultDuration(DoctorSchedule schedule, LocalDateTime start) {
        return schedule.weeklyRules().stream()
                .filter(r -> r.coversSlot(start))
                .map(r -> r.durationMinutes())
                .findFirst()
                .orElse(20);
    }
}
