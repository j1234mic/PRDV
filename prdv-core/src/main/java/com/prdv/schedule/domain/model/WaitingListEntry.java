package com.prdv.schedule.domain.model;

import com.prdv.shared.exception.ConflictException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entree liste d'attente (module 4.3). La priorisation (urgence, anciennete) est
 * exprimee par comparator() : regle metier du domaine, pas du controleur.
 */
public final class WaitingListEntry {

    public enum State { OPEN, MATCHED, EXPIRED, CANCELLED }

    private Long id;
    private final Long patientUserId;
    private final Long doctorUserId;
    private final LocalDate earliest;
    private final LocalDate latest;
    private final boolean urgent;
    private State state;
    private final LocalDateTime createdAt;

    public WaitingListEntry(Long id, Long patientUserId, Long doctorUserId, LocalDate earliest, LocalDate latest,
                            boolean urgent, State state, LocalDateTime createdAt) {
        if (earliest == null || latest == null || latest.isBefore(earliest)) {
            throw new ConflictException("Fenetre de dates invalide");
        }
        this.id = id;
        this.patientUserId = patientUserId;
        this.doctorUserId = doctorUserId;
        this.earliest = earliest;
        this.latest = latest;
        this.urgent = urgent;
        this.state = state;
        this.createdAt = createdAt;
    }

    public static WaitingListEntry join(Long patientUserId, Long doctorUserId, LocalDate earliest,
                                        LocalDate latest, boolean urgent, LocalDateTime now) {
        return new WaitingListEntry(null, patientUserId, doctorUserId, earliest, latest, urgent, State.OPEN, now);
    }

    public boolean wants(Slot slot) {
        LocalDate day = slot.start().toLocalDate();
        return !day.isBefore(earliest) && !day.isAfter(latest);
    }

    public void markMatched() {
        if (state != State.OPEN) {
            throw new ConflictException("Entree deja traitee");
        }
        this.state = State.MATCHED;
    }

    public void cancel() {
        if (state != State.OPEN) {
            throw new ConflictException("Seule une entree ouverte peut etre annulee");
        }
        this.state = State.CANCELLED;
    }

    public Long id() { return id; }
    public Long patientUserId() { return patientUserId; }
    public Long doctorUserId() { return doctorUserId; }
    public LocalDate earliest() { return earliest; }
    public LocalDate latest() { return latest; }
    public boolean urgent() { return urgent; }
    public State state() { return state; }
    public LocalDateTime createdAt() { return createdAt; }
}
