package com.prdv.schedule;

import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.ScheduleException;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.schedule.domain.model.TimeRange;
import com.prdv.schedule.domain.model.WeeklyAvailabilityRule;
import com.prdv.schedule.domain.service.SlotGenerator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le coeur est testable SANS Spring, SANS base de donnees : c'est le benefice concret
 * de l'architecture hexagonale.
 */
class SlotGeneratorTest {

    /** Lundi 7 septembre 2026 a 08:00. */
    private static final Clock NOW = Clock.fixed(
            LocalDateTime.of(2026, 9, 7, 8, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private DoctorSchedule mondaySchedule() {
        DoctorSchedule schedule = DoctorSchedule.create(1L);
        schedule.updateBookingWindow(2, 60); // preavis 2h, horizon 60j
        schedule.addRule(new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)), 20));
        return schedule;
    }

    @Test
    void grid_slots_are_generated_on_the_rule_duration() {
        WeeklyAvailabilityRule rule = new WeeklyAvailabilityRule(DayOfWeek.MONDAY,
                new TimeRange(LocalTime.of(9, 0), LocalTime.of(12, 0)), 20);
        // 9h00->12h00 = 180 min / 20 = 9 slots, ancrés sur le debut de plage
        assertEquals(9, rule.slotStarts(LocalDate.of(2026, 9, 7)).size());
    }

    @Test
    void free_slots_respect_min_notice_and_booked_overlaps() {
        SlotGenerator generator = new SlotGenerator(NOW);
        DoctorSchedule schedule = mondaySchedule();

        List<Slot> free = generator.generateFreeSlots(schedule, LocalDate.of(2026, 9, 7), 1, Set.of());
        // preavis 2h => les creneaux < 10h00 sont exclus -> 10h00, 10h20, ..., 11h40 = 6
        assertEquals(6, free.size());
        assertEquals(LocalDateTime.of(2026, 9, 7, 10, 0), free.get(0).start());

        // un creneau deja pris (10h00) et un chevauchant (10h20) sortent de la liste
        List<Slot> afterBook = generator.generateFreeSlots(schedule, LocalDate.of(2026, 9, 7), 1,
                Set.of(new Slot(LocalDateTime.of(2026, 9, 7, 10, 0), 20),
                        new Slot(LocalDateTime.of(2026, 9, 7, 10, 20), 20)));
        assertEquals(4, afterBook.size());
    }

    @Test
    void blocked_day_yields_no_slot() {
        SlotGenerator generator = new SlotGenerator(NOW);
        DoctorSchedule schedule = mondaySchedule();
        schedule.blockDay(LocalDate.of(2026, 9, 7), "Conges");

        assertTrue(generator.generateFreeSlots(schedule, LocalDate.of(2026, 9, 7), 1, Set.of()).isEmpty());
    }

    @Test
    void off_grid_starts_are_not_bookable() {
        DoctorSchedule schedule = mondaySchedule();
        LocalDateTime now = LocalDateTime.now(NOW);
        assertTrue(schedule.isBookable(new Slot(LocalDateTime.of(2026, 9, 7, 10, 0), 20), now));
        assertFalse(schedule.isBookable(new Slot(LocalDateTime.of(2026, 9, 7, 10, 10), 20), now)); // pas sur la grille
        assertFalse(schedule.isBookable(new Slot(LocalDateTime.of(2026, 9, 7, 9, 0), 20), now));   // preavis non respecte
    }

    @Test
    void slot_overlap_rule_is_symmetric() {
        Slot a = new Slot(LocalDateTime.of(2026, 9, 7, 10, 0), 20);
        Slot b = new Slot(LocalDateTime.of(2026, 9, 7, 10, 10), 20);
        Slot adjacent = new Slot(LocalDateTime.of(2026, 9, 7, 10, 20), 20);
        assertTrue(a.overlaps(b) && b.overlaps(a));
        assertFalse(a.overlaps(adjacent)); // bout-a-bout n'est pas un chevauchement
    }
}
