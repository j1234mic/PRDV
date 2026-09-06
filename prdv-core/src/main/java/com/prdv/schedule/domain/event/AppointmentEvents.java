package com.prdv.schedule.domain.event;

import com.prdv.shared.event.DomainEvent;

import java.time.LocalDateTime;

/**
 * Evenements de domaine du module RDV (OBSERVER). Un seul fichier regroupe
 * les evenements homogenes de l agenda ; chaque consumer (notification,
 * liste d'attente, analytics futur) s'y abonne sans que le booking le sache.
 */
public final class AppointmentEvents {

    private AppointmentEvents() {
    }

    public record Booked(Long appointmentId, Long patientUserId, Long doctorUserId, LocalDateTime start,
                        int durationMinutes, String type, boolean confirmed, String bookedBy) implements DomainEvent {
    }

    public record Cancelled(Long appointmentId, Long patientUserId, Long doctorUserId,
                            LocalDateTime slotStart, LocalDateTime slotEnd,
                            String cancelledBy, int feeCents) implements DomainEvent {
    }

    public record Rescheduled(Long appointmentId, Long patientUserId, Long doctorUserId,
                              LocalDateTime oldStart, LocalDateTime newStart) implements DomainEvent {
    }

    public record ConfirmedByDoctor(Long appointmentId, Long patientUserId, Long doctorUserId,
                                    LocalDateTime start) implements DomainEvent {
    }
}
