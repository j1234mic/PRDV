package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.in.GetScheduleUseCase;
import com.prdv.schedule.application.port.in.UpdateScheduleUseCase;
import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.ScheduleException;
import com.prdv.schedule.domain.model.TimeRange;
import com.prdv.schedule.domain.model.WeeklyAvailabilityRule;
import com.prdv.schedule.domain.policy.CancellationMode;
import com.prdv.shared.exception.NotFoundException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/** Configuration agenda (module 4.1). Un seul agregat par methodes : les regles restent dans DoctorSchedule. */
public final class ScheduleHandler implements UpdateScheduleUseCase, GetScheduleUseCase {

    private final DoctorScheduleRepository schedules;

    public ScheduleHandler(DoctorScheduleRepository schedules) {
        this.schedules = schedules;
    }

    @Override
    public DoctorSchedule addRule(Long doctorUserId, RuleDto rule) {
        DoctorSchedule schedule = loadOrCreate(doctorUserId);
        schedule.addRule(new WeeklyAvailabilityRule(rule.day(), new TimeRange(rule.start(), rule.end()),
                rule.durationMinutes()));
        return schedules.save(schedule);
    }

    @Override
    public DoctorSchedule removeRule(Long doctorUserId, DayOfWeek day, LocalTime rangeStart) {
        DoctorSchedule schedule = load(doctorUserId);
        schedule.weeklyRules().stream()
                .filter(r -> r.day() == day && r.range().start().equals(rangeStart))
                .findFirst()
                .ifPresentOrElse(schedule::removeRule,
                        () -> {
                            throw new NotFoundException("Plage horaire introuvable");
                        });
        return schedules.save(schedule);
    }

    @Override
    public DoctorSchedule blockDay(Long doctorUserId, LocalDate date, String reason) {
        DoctorSchedule schedule = load(doctorUserId);
        schedule.blockDay(date, reason);
        return schedules.save(schedule);
    }

    @Override
    public DoctorSchedule unblockDay(Long doctorUserId, LocalDate date) {
        DoctorSchedule schedule = load(doctorUserId);
        schedule.unblockDay(date);
        return schedules.save(schedule);
    }

    @Override
    public DoctorSchedule updateSettings(Long doctorUserId, int minNoticeHours, int maxHorizonDays,
                                         CancellationMode mode, boolean requiresManualConfirmation) {
        DoctorSchedule schedule = load(doctorUserId);
        schedule.updateBookingWindow(minNoticeHours, maxHorizonDays);
        schedule.updateCancellationMode(mode);
        schedule.setRequiresManualConfirmation(requiresManualConfirmation);
        return schedules.save(schedule);
    }

    @Override
    public DoctorSchedule forDoctor(Long doctorUserId) {
        return loadOrCreate(doctorUserId);
    }

    private DoctorSchedule loadOrCreate(Long doctorUserId) {
        return schedules.findByDoctorUserId(doctorUserId).orElseGet(() -> DoctorSchedule.create(doctorUserId));
    }

    private DoctorSchedule load(Long doctorUserId) {
        return schedules.findByDoctorUserId(doctorUserId)
                .orElseThrow(() -> new NotFoundException("Agenda introuvable : ajoutez d'abord une plage horaire"));
    }
}
