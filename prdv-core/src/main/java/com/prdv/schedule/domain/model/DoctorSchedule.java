package com.prdv.schedule.domain.model;

import com.prdv.schedule.domain.policy.CancellationPolicy;
import com.prdv.schedule.domain.policy.CancellationPolicyFactory;
import com.prdv.schedule.domain.policy.CancellationMode;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.NotFoundException;
import com.prdv.shared.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AGREGAT RACINE de l'agenda d'un praticien (module 4.1).
 * Porte les regles "Delai minimum de reservation", "Delai maximum", exceptions,
 * et delivre la politique d'annulation applicable (STRATEGY resolue par une Factory).
 */
public final class DoctorSchedule {

    private Long id;
    private final Long doctorUserId;
    private final List<WeeklyAvailabilityRule> weeklyRules = new ArrayList<>();
    private final List<ScheduleException> exceptions = new ArrayList<>();
    private int minNoticeHours;
    private int maxHorizonDays;
    private CancellationMode cancellationMode;
    private boolean requiresManualConfirmation;

    public DoctorSchedule(Long id, Long doctorUserId, List<WeeklyAvailabilityRule> weeklyRules,
                          List<ScheduleException> exceptions, int minNoticeHours, int maxHorizonDays,
                          CancellationMode cancellationMode, boolean requiresManualConfirmation) {
        this.id = id;
        this.doctorUserId = doctorUserId;
        if (weeklyRules != null) {
            this.weeklyRules.addAll(weeklyRules);
        }
        if (exceptions != null) {
            this.exceptions.addAll(exceptions);
        }
        this.minNoticeHours = minNoticeHours;
        this.maxHorizonDays = maxHorizonDays;
        this.cancellationMode = cancellationMode == null ? CancellationMode.STANDARD_24H : cancellationMode;
        this.requiresManualConfirmation = requiresManualConfirmation;
    }

    public static DoctorSchedule create(Long doctorUserId) {
        return new DoctorSchedule(null, doctorUserId, List.of(), List.of(), 2, 60,
                CancellationMode.STANDARD_24H, false);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Identifiant deja affecte");
        }
        this.id = id;
    }

    // ----- configuration ------------------------------------------------------

    public void addRule(WeeklyAvailabilityRule rule) {
        boolean duplicate = weeklyRules.stream().anyMatch(r -> r.day() == rule.day() && r.range().equals(rule.range()));
        if (duplicate) {
            throw new ConflictException("Plage deja configurée pour ce jour");
        }
        weeklyRules.add(rule);
    }

    public void removeRule(WeeklyAvailabilityRule rule) {
        if (!weeklyRules.remove(rule)) {
            throw new NotFoundException("Plage horaire introuvable");
        }
    }

    public void blockDay(LocalDate date, String reason) {
        exceptions.add(ScheduleException.fullDay(date, reason));
    }

    public void unblockDay(LocalDate date) {
        if (!exceptions.removeIf(e -> e.date().equals(date) && e.isFullDay())) {
            throw new NotFoundException("Aucun blocage ce jour-la");
        }
    }

    public void updateCancellationMode(CancellationMode mode) {
        this.cancellationMode = mode;
    }

    public void updateBookingWindow(int minNoticeHours, int maxHorizonDays) {
        if (minNoticeHours < 0 || maxHorizonDays < 1) {
            throw new ValidationException("Fenetres de reservation invalides");
        }
        this.minNoticeHours = minNoticeHours;
        this.maxHorizonDays = maxHorizonDays;
    }

    public void setRequiresManualConfirmation(boolean value) {
        this.requiresManualConfirmation = value;
    }

    // ----- regles ---------------------------------------------------------------

    /** Un creneau est OPENABLE si : sur la grille d'une regle, dans la fenetre [now+notice, now+horizon], non bloque. */
    public boolean isBookable(Slot slot, LocalDateTime now) {
        if (slot.start().isBefore(now.plusHours(minNoticeHours))) {
            return false;
        }
        if (slot.start().toLocalDate().isAfter(now.toLocalDate().plusDays(maxHorizonDays))) {
            return false;
        }
        if (isBlocked(slot.start().toLocalDate())) {
            return false;
        }
        return weeklyRules.stream().anyMatch(r -> r.coversSlot(slot.start()));
    }

    public boolean isBlocked(LocalDate date) {
        return exceptions.stream().anyMatch(e -> e.isFullDay() && e.appliesTo(date));
    }

    /** Politique applicable aux annulations/deplacements sur cet agenda. */
    public CancellationPolicy cancellationPolicy() {
        return CancellationPolicyFactory.forMode(cancellationMode);
    }

    public Long id() { return id; }
    public Long doctorUserId() { return doctorUserId; }
    public List<WeeklyAvailabilityRule> weeklyRules() { return List.copyOf(weeklyRules); }
    public List<ScheduleException> exceptions() { return List.copyOf(exceptions); }
    public int minNoticeHours() { return minNoticeHours; }
    public int maxHorizonDays() { return maxHorizonDays; }
    public CancellationMode cancellationMode() { return cancellationMode; }
    public boolean requiresManualConfirmation() { return requiresManualConfirmation; }
}
