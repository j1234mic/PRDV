package com.prdv.schedule.domain.model;

import java.time.LocalDate;

/** Conge / jour ferie / salle indisponible : neutralise tout le jour ou un intervalle [start,end). */
public record ScheduleException(LocalDate date, LocalDate until, java.time.LocalTime start,
                                java.time.LocalTime end, String reason) {

    public static ScheduleException fullDay(LocalDate date, String reason) {
        return new ScheduleException(date, date, null, null, reason);
    }

    public boolean appliesTo(LocalDate day) {
        LocalDate to = until == null ? date : until;
        return !day.isBefore(date) && !day.isAfter(to);
    }

    public boolean isFullDay() {
        return start == null || end == null;
    }
}
