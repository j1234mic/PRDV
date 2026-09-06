package com.prdv.schedule.domain.model;

import com.prdv.shared.exception.ValidationException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plage hebdomadaire (module 4.1 "Horaires par jour" + "Duree par type de consultation"
 * via durationMinutes). Generateur de la GRILLE de creneaux ancree sur le debut de plage.
 */
public record WeeklyAvailabilityRule(DayOfWeek day, TimeRange range, int durationMinutes) {

    public WeeklyAvailabilityRule {
        if (day == null || range == null || durationMinutes < 5) {
            throw new ValidationException("Regle d'agenda invalide (duree >= 5 min)");
        }
        if (range.minutes() % durationMinutes != 0) {
            throw new ValidationException("La duree du creneau doit diviser la plage horaire");
        }
    }

    /** Tous les creneaux de la grille pour une date donnee (sans controle de conflit). */
    public List<LocalDateTime> slotStarts(LocalDate date) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime cursor = date.atTime(range.start());
        LocalDateTime stop = date.atTime(range.end());
        while (!cursor.plusMinutes(durationMinutes).isAfter(stop)) {
            slots.add(cursor);
            cursor = cursor.plusMinutes(durationMinutes);
        }
        return slots;
    }

    /** Vrai si (start, duree) est exactement un creneau de cette regle (grille ancree sur le debut de plage). */
    public boolean coversSlot(LocalDateTime slotStart) {
        if (slotStart.getDayOfWeek() != day) {
            return false;
        }
        LocalDateTime dayStart = slotStart.toLocalDate().atTime(range.start());
        LocalDateTime dayEnd = slotStart.toLocalDate().atTime(range.end());
        if (slotStart.isBefore(dayStart) || slotStart.plusMinutes(durationMinutes).isAfter(dayEnd)) {
            return false;
        }
        return Duration.between(dayStart, slotStart).toMinutes() % durationMinutes == 0;
    }
}
