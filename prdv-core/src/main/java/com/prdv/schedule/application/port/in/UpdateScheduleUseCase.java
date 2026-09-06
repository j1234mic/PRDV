package com.prdv.schedule.application.port.in;

import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.policy.CancellationMode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public interface UpdateScheduleUseCase {

    record RuleDto(DayOfWeek day, LocalTime start, LocalTime end, int durationMinutes) { }

    DoctorSchedule addRule(Long doctorUserId, RuleDto rule);
    DoctorSchedule removeRule(Long doctorUserId, DayOfWeek day, LocalTime rangeStart);
    DoctorSchedule blockDay(Long doctorUserId, LocalDate date, String reason);
    DoctorSchedule unblockDay(Long doctorUserId, LocalDate date);
    DoctorSchedule updateSettings(Long doctorUserId, int minNoticeHours, int maxHorizonDays,
                                  CancellationMode mode, boolean requiresManualConfirmation);
}
