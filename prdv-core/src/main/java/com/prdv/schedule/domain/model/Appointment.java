package com.prdv.schedule.domain.model;

import com.prdv.schedule.domain.policy.CancellationPolicy;
import com.prdv.shared.exception.ConflictException;
import com.prdv.shared.exception.DomainException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AGREGAT RACINE "Rendez-vous" (module 4.2/4.3). Toutes les transitions d'etat passent
 * par des methodes qui protegent les invariants : pas de setStatus() public (encapsulation,
 * base du "State" porte par l'entite).
 */
public final class Appointment {

    private Long id;
    private final Long patientUserId;
    private final Long doctorUserId;
    private Slot slot;
    private final AppointmentType type;
    private AppointmentStatus status;
    private String reason;
    private String teleconsultationUrl;
    private int cancellationFeeCents;
    private String cancellationReason;
    private String bookedBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Appointment(Long id, Long patientUserId, Long doctorUserId, Slot slot, AppointmentType type,
                        AppointmentStatus status, String reason, String teleconsultationUrl,
                        int cancellationFeeCents, String cancellationReason, String bookedBy,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.patientUserId = patientUserId;
        this.doctorUserId = doctorUserId;
        this.slot = slot;
        this.type = type;
        this.status = status;
        this.reason = reason;
        this.teleconsultationUrl = teleconsultationUrl;
        this.cancellationFeeCents = cancellationFeeCents;
        this.cancellationReason = cancellationReason;
        this.bookedBy = bookedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** FACTORY : nouvelle demande ; l'auto-confirmation depend du reglage du medecin. */
    public static Appointment request(Long patientUserId, Long doctorUserId, Slot slot, AppointmentType type,
                                      String reason, boolean autoConfirmed, String bookedBy, LocalDateTime now) {
        if (slot.start().isBefore(now)) {
            throw new ConflictException("Impossible de reserver dans le passe");
        }
        return new Appointment(null, patientUserId, doctorUserId, slot, type,
                autoConfirmed ? AppointmentStatus.CONFIRMED : AppointmentStatus.PENDING,
                reason, null, 0, null, bookedBy, now, now);
    }

    public static Appointment restore(Long id, Long patientUserId, Long doctorUserId, Slot slot, AppointmentType type,
                                      AppointmentStatus status, String reason, String teleconsultationUrl,
                                      int cancellationFeeCents, String cancellationReason, String bookedBy,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Appointment(id, patientUserId, doctorUserId, slot, type, status, reason, teleconsultationUrl,
                cancellationFeeCents, cancellationReason, bookedBy, createdAt, updatedAt);
    }

    // ----- transitions ------------------------------------------------------

    public void confirm(LocalDateTime now) {
        requireStatus(AppointmentStatus.PENDING);
        this.status = AppointmentStatus.CONFIRMED;
        touch(now);
    }

    /** Annulation patient : autorisee a tout moment avant le RDV, penalite selon la STRATEGY. */
    public void cancelByPatient(CancellationPolicy policy, String reason, LocalDateTime now) {
        cancel(policy, reason, AppointmentStatus.CANCELLED_BY_PATIENT, now);
    }

    /** Le medecin peut toujours annuler (il libere son agenda) : pas de penalite. */
    public void cancelByDoctor(String reason, LocalDateTime now) {
        cancel(null, reason, AppointmentStatus.CANCELLED_BY_DOCTOR, now);
    }

    private void cancel(CancellationPolicy policy, String reason, AppointmentStatus newStatus, LocalDateTime now) {
        if (!status.occupiesSlot()) {
            throw new ConflictException("Ce rendez-vous n'est plus annulable (statut " + status + ")");
        }
        if (!now.isBefore(slot.start())) {
            throw new ConflictException("Le rendez-vous a deja commence : contactez le cabinet");
        }
        if (policy != null && policy.isLate(slot.start(), now)) {
            this.cancellationFeeCents = policy.cancellationFeeCents();
        }
        this.cancellationReason = reason;
        this.status = newStatus;
        touch(now);
    }

    /** Deplacement conditionnel (module 4.2 "Modification de creneau selon regles"). */
    public void rescheduleTo(Slot newSlot, CancellationPolicy policy, LocalDateTime now) {
        if (!status.occupiesSlot()) {
            throw new ConflictException("RDV non deplacable (statut " + status + ")");
        }
        if (newSlot.start().isBefore(now)) {
            throw new ConflictException("Impossible de deplacer dans le passe");
        }
        if (!policy.isRescheduleAllowed(slot.start(), now)) {
            throw new ConflictException("Trop tard pour deplacer : annulez ou appelez le secretaire");
        }
        this.slot = newSlot;
        touch(now);
    }

    public void complete(LocalDateTime now) {
        requireStatus(AppointmentStatus.CONFIRMED);
        this.status = AppointmentStatus.COMPLETED;
        touch(now);
    }

    public void markNoShow(LocalDateTime now) {
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.PENDING) {
            throw new ConflictException("Pas de no-show sur ce statut");
        }
        if (now.isBefore(slot.start())) {
            throw new ConflictException("Le RDV n'est pas encore passe");
        }
        this.status = AppointmentStatus.NO_SHOW;
        touch(now);
    }

    public void attachTeleconsultationUrl(String url, LocalDateTime now) {
        if (type != AppointmentType.TELECONSULTATION) {
            throw new DomainException("Lien de teleconsultation reserve aux RDV de teleconsultation");
        }
        this.teleconsultationUrl = url;
        touch(now);
    }

    public String joinUrl() {
        if (teleconsultationUrl != null) {
            return teleconsultationUrl;
        }
        return "https://visio.prdv.local/rooms/" + (id == null ? UUID.randomUUID() : id);
    }

    private void requireStatus(AppointmentStatus expected) {
        if (status != expected) {
            throw new ConflictException("Transition interdite depuis le statut " + status);
        }
    }

    private void touch(LocalDateTime now) {
        this.updatedAt = now;
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("Identifiant deja affecte");
        }
        this.id = id;
    }

    // ----- getters ------------------------------------------------------------

    public Long id() { return id; }
    public Long patientUserId() { return patientUserId; }
    public Long doctorUserId() { return doctorUserId; }
    public Slot slot() { return slot; }
    public AppointmentType type() { return type; }
    public AppointmentStatus status() { return status; }
    public String reason() { return reason; }
    public String teleconsultationUrl() { return teleconsultationUrl; }
    public int cancellationFeeCents() { return cancellationFeeCents; }
    public String cancellationReason() { return cancellationReason; }
    public String bookedBy() { return bookedBy; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
