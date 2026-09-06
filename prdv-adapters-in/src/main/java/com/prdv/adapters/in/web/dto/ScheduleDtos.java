package com.prdv.adapters.in.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public final class ScheduleDtos {

    private ScheduleDtos() {
    }

    public record RuleRequest(@NotNull DayOfWeek day, @NotNull LocalTime start, @NotNull LocalTime end,
                              @Min(5) int durationMinutes) {
    }

    public record BlockDayRequest(@NotNull LocalDate date, String reason) {
    }

    public record SettingsRequest(@Min(0) int minNoticeHours, @Min(1) int maxHorizonDays,
                                  @NotNull String cancellationMode, boolean requiresManualConfirmation) {
    }

    public record RuleResponse(DayOfWeek day, LocalTime start, LocalTime end, int durationMinutes) {
    }

    public record ExceptionResponse(LocalDate date, LocalDate until, String reason) {
    }

    public record ScheduleResponse(Long doctorUserId, List<RuleResponse> weeklyRules,
                                   List<ExceptionResponse> exceptions, int minNoticeHours, int maxHorizonDays,
                                   String cancellationMode, boolean requiresManualConfirmation) {
    }

    public record SlotResponse(LocalDateTime start, LocalDateTime end, int durationMinutes) {
    }

    public record BookingRequest(@NotNull Long doctorId, @NotNull LocalDateTime start,
                                 @NotNull String type, String reason) {
    }

    public record RescheduleRequest(@NotNull LocalDateTime newStart) {
    }

    public record CancelRequest(String reason) {
    }

    public record AppointmentResponse(Long id, Long patientUserId, Long doctorUserId, LocalDateTime start,
                                      LocalDateTime end, int durationMinutes, String type, String status,
                                      String reason, String teleconsultationUrl, int cancellationFeeCents,
                                      String bookedBy) {
    }

    public record WaitlistRequest(@NotNull Long doctorId, @NotNull LocalDate earliest, @NotNull LocalDate latest,
                                 boolean urgent) {
    }
}
