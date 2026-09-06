package com.prdv.schedule.domain.service;

import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.schedule.domain.model.WeeklyAvailabilityRule;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * SERVICE DE DOMAINE (stateless) : materialise la promesse "Calendrier disponibilites"
 * (module 4.2 etape 3). Recoit un Clock (injection) : testable a date figee, 100% pur.
 */
public final class SlotGenerator {

    private final Clock clock;

    public SlotGenerator(Clock clock) {
        this.clock = clock;
    }

    public List<LocalDate> openDays(DoctorSchedule schedule, LocalDate from, int daysAhead) {
        LocalDate start = from.isBefore(LocalDate.now(clock)) ? LocalDate.now(clock) : from;
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < daysAhead; i++) {
            LocalDate day = start.plusDays(i);
            if (hasRuleOn(schedule, day) && !schedule.isBlocked(day)) {
                days.add(day);
            }
        }
        return days;
    }

    public List<Slot> generateFreeSlots(DoctorSchedule schedule, LocalDate from, int daysAhead,
                                        Set<Slot> alreadyBooked) {
        Set<Slot> booked = new HashSet<>(alreadyBooked);
        LocalDateTime now = LocalDateTime.now(clock);
        List<Slot> free = new ArrayList<>();
        for (LocalDate day : openDays(schedule, from, daysAhead)) {
            for (Slot slot : rawSlotsOn(schedule, day)) {
                if (schedule.isBookable(slot, now) && booked.stream().noneMatch(slot::overlaps)) {
                    free.add(slot);
                }
            }
        }
        free.sort(Comparator.comparing(Slot::start));
        return List.copyOf(free);
    }

    private List<Slot> rawSlotsOn(DoctorSchedule schedule, LocalDate day) {
        List<Slot> slots = new ArrayList<>();
        for (WeeklyAvailabilityRule rule : schedule.weeklyRules()) {
            if (rule.day() != day.getDayOfWeek()) {
                continue;
            }
            rule.slotStarts(day).forEach(start -> slots.add(new Slot(start, rule.durationMinutes())));
        }
        return slots;
    }

    private boolean hasRuleOn(DoctorSchedule schedule, LocalDate day) {
        return schedule.weeklyRules().stream().anyMatch(r -> r.day() == day.getDayOfWeek());
    }
}
