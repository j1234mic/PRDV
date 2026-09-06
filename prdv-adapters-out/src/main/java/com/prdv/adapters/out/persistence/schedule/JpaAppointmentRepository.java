package com.prdv.adapters.out.persistence.schedule;

import com.prdv.schedule.application.port.out.AppointmentRepository;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.AppointmentStatus;
import com.prdv.schedule.domain.model.Slot;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class JpaAppointmentRepository implements AppointmentRepository {

    private static final List<AppointmentStatus> ACTIVE = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);

    private final AppointmentJpaRepository jpa;
    private final DoctorScheduleJpaRepository schedules;

    public JpaAppointmentRepository(AppointmentJpaRepository jpa, DoctorScheduleJpaRepository schedules) {
        this.jpa = jpa;
        this.schedules = schedules;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity e = new AppointmentEntity();
        e.setId(appointment.id());
        e.setPatientUserId(appointment.patientUserId());
        e.setDoctorUserId(appointment.doctorUserId());
        e.setStart(appointment.slot().start());
        e.setEnd(appointment.slot().end());
        e.setDurationMinutes(appointment.slot().durationMinutes());
        e.setType(appointment.type());
        e.setStatus(appointment.status());
        e.setReason(appointment.reason());
        e.setTeleconsultationUrl(appointment.teleconsultationUrl());
        e.setCancellationFeeCents(appointment.cancellationFeeCents());
        e.setCancellationReason(appointment.cancellationReason());
        e.setBookedBy(appointment.bookedBy());
        e.setCreatedAt(appointment.createdAt());
        e.setUpdatedAt(appointment.updatedAt());
        AppointmentEntity saved = jpa.save(e);
        if (appointment.id() == null) {
            appointment.assignId(saved.getId());
        }
        return appointment;
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return jpa.findById(id).map(JpaAppointmentRepository::toDomain);
    }

    @Override
    public List<Appointment> findActiveOverlapping(Long doctorUserId, LocalDateTime start, LocalDateTime end) {
        return jpa.findActiveOverlapping(doctorUserId, ACTIVE, start, end).stream()
                .map(JpaAppointmentRepository::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByPatient(Long patientUserId, int limit) {
        return jpa.findByPatientUserIdOrderByStartDesc(patientUserId, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(JpaAppointmentRepository::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findByDoctorBetween(Long doctorUserId, LocalDateTime from, LocalDateTime to) {
        return jpa.findForDoctorBetween(doctorUserId, from, to).stream()
                .map(JpaAppointmentRepository::toDomain)
                .toList();
    }

    @Override
    public void lockDoctorSchedule(Long doctorUserId) {
        // SELECT ... FOR UPDATE ; no-op si aucun agenda (le bloc sera cree dans la transaction).
        schedules.lockByDoctorUserId(doctorUserId);
    }

    private static Appointment toDomain(AppointmentEntity e) {
        int duration = e.getDurationMinutes() > 0 ? e.getDurationMinutes()
                : (int) Duration.between(e.getStart(), e.getEnd()).toMinutes();
        return Appointment.restore(e.getId(), e.getPatientUserId(), e.getDoctorUserId(),
                new Slot(e.getStart(), duration), e.getType(), e.getStatus(), e.getReason(),
                e.getTeleconsultationUrl(), e.getCancellationFeeCents(), e.getCancellationReason(),
                e.getBookedBy(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
