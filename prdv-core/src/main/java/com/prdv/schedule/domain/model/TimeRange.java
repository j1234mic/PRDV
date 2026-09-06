package com.prdv.schedule.domain.model;

import com.prdv.shared.exception.ValidationException;

import java.time.LocalTime;

/** Value Object plage horaire [start, end). */
public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ValidationException("Plage horaire invalide (end doit suivre start)");
        }
    }

    public long minutes() {
        return java.time.Duration.between(start, end).toMinutes();
    }

    public boolean contains(LocalTime t) {
        return !t.isBefore(start) && t.isBefore(end);
    }
}
