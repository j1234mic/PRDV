package com.prdv.schedule.application.service;

import com.prdv.schedule.application.port.out.AppointmentRepository;
import com.prdv.schedule.application.port.out.DoctorScheduleRepository;
import com.prdv.schedule.application.port.out.WaitingListRepository;
import com.prdv.schedule.domain.event.AppointmentEvents;
import com.prdv.schedule.domain.model.Appointment;
import com.prdv.schedule.domain.model.AppointmentType;
import com.prdv.schedule.domain.model.DoctorSchedule;
import com.prdv.schedule.domain.model.Slot;
import com.prdv.schedule.domain.model.WaitingListEntry;
import com.prdv.shared.event.DomainEvent;
import com.prdv.shared.event.DomainEventHandler;
import com.prdv.shared.event.DomainEventPublisher;
import com.prdv.notification.application.service.NotificationService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pattern OBSERVER : "Notification automatique si liberation + attribution automatique"
 * (module 4.3). Ce handler ecoute l'annulation, choisit le patient prioritaire
 * (urgence > anciennete) via une STRATEGY de comparaison, et re-reserve pour lui.
 * Aucun composant d'annulation ne le connait -> Open/Closed.
 */
public final class WaitlistAutoAssignHandler implements DomainEventHandler {

    private static final DateTimeFormatter SLOT_FORMAT =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy 'a' HH:mm", Locale.FRENCH);

    private final WaitingListRepository waitingList;
    private final AppointmentRepository appointments;
    private final DoctorScheduleRepository schedules;
    private final DomainEventPublisher events;
    private final NotificationService notifications;
    private final Clock clock;

    public WaitlistAutoAssignHandler(WaitingListRepository waitingList, AppointmentRepository appointments,
                                     DoctorScheduleRepository schedules, DomainEventPublisher events,
                                     NotificationService notifications, Clock clock) {
        this.waitingList = waitingList;
        this.appointments = appointments;
        this.schedules = schedules;
        this.events = events;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    public boolean supports(DomainEvent event) {
        // Seules les annulations du medecin déclenchent un re-assignement automatique :
        // une annulation patient tardive (penalisee) est traitable par le secretaire.
        return event instanceof AppointmentEvents.Cancelled cancelled
                && "DOCTOR".equals(cancelled.cancelledBy());
    }

    @Override
    public void handle(DomainEvent event) {
        AppointmentEvents.Cancelled cancelled = (AppointmentEvents.Cancelled) event;
        LocalDateTime now = LocalDateTime.now(clock);
        int duration = (int) Duration.between(cancelled.slotStart(), cancelled.slotEnd()).toMinutes();
        Slot freed = new Slot(cancelled.slotStart(), duration);

        Optional<WaitingListEntry> candidate = bestCandidate(cancelled.doctorUserId(), freed, now);
        if (candidate.isEmpty()) {
            return;
        }
        WaitingListEntry entry = candidate.get();
        DoctorSchedule schedule = schedules.findByDoctorUserId(cancelled.doctorUserId())
                .orElseGet(() -> DoctorSchedule.create(cancelled.doctorUserId()));
        if (!schedule.isBookable(freed, now)) {
            return; // entre-temps l'agenda a change
        }

        appointments.lockDoctorSchedule(cancelled.doctorUserId());
        if (!appointments.findActiveOverlapping(cancelled.doctorUserId(), freed.start(), freed.end()).isEmpty()) {
            return; // un autre patient a pris le creneau entre-temps
        }

        Appointment appointment = Appointment.request(entry.patientUserId(), cancelled.doctorUserId(), freed,
                AppointmentType.FOLLOW_UP, "Attribution automatique liste d'attente", true, "WAITLIST", now);
        appointment = appointments.save(appointment);
        entry.markMatched();
        waitingList.save(entry);

        events.publish(new AppointmentEvents.Booked(appointment.id(), appointment.patientUserId(),
                appointment.doctorUserId(), freed.start(), freed.durationMinutes(),
                AppointmentType.FOLLOW_UP.name(), true, "WAITLIST"));
        notifications.notifyWaitlistMatched(entry.patientUserId(), freed.start().format(SLOT_FORMAT));
    }

    /** Priorisation "urgence, anciennete" (module 4.3) : comparateur injectable = STRATEGY. */
    private Optional<WaitingListEntry> bestCandidate(Long doctorUserId, Slot freed, LocalDateTime now) {
        Comparator<WaitingListEntry> priority = Comparator
                .comparing((WaitingListEntry e) -> !e.urgent())
                .thenComparing(WaitingListEntry::createdAt);
        return waitingList.findOpenByDoctor(doctorUserId).stream()
                .filter(e -> e.state() == WaitingListEntry.State.OPEN)
                .filter(e -> e.wants(freed))
                .filter(e -> freed.start().isAfter(now.plusHours(2))) // delai de prevention minimum
                .sorted(priority)
                .findFirst();
    }
}
