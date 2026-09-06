package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.FindSlotsUseCase;
import com.prdv.schedule.application.port.out.AppointmentRepository;
import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.schedule.domain.service.SlotGenerator;
import com.prdv.shared.exception.NotFoundException;
import com.prdv.shared.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Composition simple : le generator pur (domain service) fournit la theorie,
 * le repository fournit la pratique (creneaux deja pris). Le handler ne connait
 * NI JPA NI HTTP -> test unitaire avec mocks.
 */
public final class FindSlotsHandler implements FindSlotsUseCase {

    private static final int MAX_WINDOW_DAYS = 120;

    private final DoctorScheduleRepository schedules;
    private final AppointmentRepository appointments;
    private final SlotGenerator generator;

    public FindSlotsHandler(DoctorScheduleRepository schedules, AppointmentRepository appointments,
                            SlotGenerator generator) {
        this.schedules = schedules;
        this.appointments = appointments;
        this.generator = generator;
    }

    @Override
    public List<Slot> findFreeSlots(Long doctorUserId, LocalDate from, int days) {
        DoctorSchedule schedule = load(doctorUserId);
        LocalDate start = from == null ? LocalDate.now() : from;
        int window = Math.min(Math.max(days, 1), MAX_WINDOW_DAYS);

        LocalDateTime to = start.plusDays(window).atStartOfDay();
        Set<Slot> booked = appointments.findByDoctorBetween(doctorUserId, start.atStartOfDay(), to).stream()
                .map(Appointment::slot)
                .collect(Collectors.toSet());
        return generator.generateFreeSlots(schedule, start, window, booked);
    }

    @Override
    public Slot firstAvailable(Long doctorUserId, LocalDate from) {
        List<Slot> slots = findFreeSlots(doctorUserId, from, 14);
        if (slots.isEmpty()) {
            throw new NotFoundException("Aucun creneau libre dans les 14 prochains jours");
        }
        return slots.get(0);
    }

    private DoctorSchedule load(Long doctorUserId) {
        return schedules.findByDoctorUserId(doctorUserId)
                .orElseThrow(() -> new NotFoundException("Cet utilisateur n'a pas d'agenda configure"));
    }
}
