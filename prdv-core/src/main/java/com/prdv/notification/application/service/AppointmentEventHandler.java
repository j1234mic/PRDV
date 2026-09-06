package com.prdv.notification.application.service;

import com.prdv.notification.application.port.out.AuditLog;
import com.prdv.profile.domain.event.DoctorProfileValidatedEvent;
import com.prdv.schedule.domain.event.AppointmentEvents;
import com.prdv.shared.event.DomainEvent;
import com.prdv.shared.event.DomainEventHandler;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Observateur "notifications" (module 5.1). Il ne fait QUE traduire un evenement en
 * message : la regle metier reste du cote du booking. Les evenements non compris
 * sont silently ignores -> on peut en ajouter a volonte sans toucher ce fichier.
 */
public final class AppointmentEventHandler implements DomainEventHandler {

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'a' HH:mm", Locale.FRENCH);

    private final NotificationService notifications;
    private final AuditLog audit;

    public AppointmentEventHandler(NotificationService notifications, AuditLog audit) {
        this.notifications = notifications;
        this.audit = audit;
    }

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof AppointmentEvents.Booked
                || event instanceof AppointmentEvents.Cancelled
                || event instanceof AppointmentEvents.Rescheduled
                || event instanceof AppointmentEvents.ConfirmedByDoctor
                || event instanceof DoctorProfileValidatedEvent;
    }

    @Override
    public void handle(DomainEvent event) {
        if (event instanceof AppointmentEvents.Booked e) {
            notifications.notifyAppointmentBooked(e.patientUserId(), e.doctorUserId(),
                    e.start().format(SLOT_FORMAT), e.confirmed());
            audit.record("APPOINTMENT_BOOKED", String.valueOf(e.appointmentId()));
        } else if (event instanceof AppointmentEvents.Cancelled e) {
            notifications.notifyAppointmentCancelled(e.patientUserId(), e.doctorUserId(),
                    e.slotStart().format(SLOT_FORMAT), e.cancelledBy(), e.feeCents());
            audit.record("APPOINTMENT_CANCELLED", e.appointmentId() + " par " + e.cancelledBy());
        } else if (event instanceof AppointmentEvents.Rescheduled e) {
            notifications.notifyRescheduled(e.patientUserId(), e.doctorUserId(),
                    e.oldStart().format(SLOT_FORMAT), e.newStart().format(SLOT_FORMAT));
            audit.record("APPOINTMENT_RESCHEDULED", String.valueOf(e.appointmentId()));
        } else if (event instanceof AppointmentEvents.ConfirmedByDoctor e) {
            notifications.notifyConfirmed(e.patientUserId(), e.start().format(SLOT_FORMAT));
            audit.record("APPOINTMENT_CONFIRMED", String.valueOf(e.appointmentId()));
        } else if (event instanceof DoctorProfileValidatedEvent e) {
            notifications.notifyDoctorProfileDecision(e.doctorUserId(), e.approved(), e.reason());
            audit.record("DOCTOR_PROFILE_" + (e.approved() ? "APPROVED" : "REJECTED"),
                    String.valueOf(e.doctorUserId()));
        }
    }
}
