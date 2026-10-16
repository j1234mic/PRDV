package com.prdv.rdv.planning.application.port.input;

import com.prdv.rdv.planning.domain.Agenda;
import java.time.*;
import java.util.*;

public interface PlanningUseCase {
    /** Opens this practitioner's agenda; returns true only when it was newly created. */
    boolean open();
    Agenda importCalendar(String content,String site);
    List<com.prdv.rdv.planning.domain.WaitingOffer> offers(Long practitioner);
    Agenda.Appointment acceptOffer(Long practitioner,UUID offer,Agenda.Intake intake);
    void declineOffer(Long practitioner,UUID offer);
    com.prdv.rdv.planning.domain.AgendaSettings settings();
    void saveTemplate(String name, com.prdv.rdv.planning.domain.SchedulePattern pattern);
    void deleteTemplate(String name);
    Agenda applyTemplate(String name, LocalDate from, LocalDate until);
    void saveReason(String id, com.prdv.rdv.planning.domain.ConsultationReason reason);
    void deleteReason(String id);
    List<com.prdv.rdv.planning.domain.ConsultationReason> reasons(Long practitioner);
    Agenda duplicate(Instant from, Instant to, int shiftDays);
    Agenda close(String site, Instant from, Instant to, Agenda.Type type);
    void delay(String site, int minutes);
    Agenda.Rules rules();
    List<Agenda.Slot> slots();
    void removeSlot(UUID slot);
    AppointmentDetails details(Long practitioner, UUID appointment);
    record AppointmentDetails(Agenda.Appointment appointment, Agenda.Slot slot) {}
    Agenda configure(Agenda.Rules rules);
    Agenda add(List<Agenda.Slot> slots);
    Agenda generate(com.prdv.rdv.planning.domain.SchedulePattern pattern);
    List<Agenda.Slot> available(Long practitioner, Instant from, Instant to, String site);
    Agenda.Appointment book(Long practitioner, UUID slot, Agenda.Intake intake, boolean firstVisit);
    Agenda.Appointment cancel(Long practitioner, UUID appointment, String reason);
    Agenda.Appointment reschedule(Long practitioner, UUID appointment, UUID target, boolean firstVisit);
    Agenda.Appointment transition(Long practitioner, UUID appointment, Agenda.Status status);
    List<Agenda.Appointment> appointments(Long practitioner);
    Agenda.Waiting join(Long practitioner, Instant from, Instant to, String site, Agenda.Type type, boolean urgent, boolean automaticOffer, String communication);
    List<Agenda.Waiting> candidates(UUID slot);
    void leave(Long practitioner);
    Agenda.Appointment assign(UUID waiting, UUID slot, Agenda.Intake intake, boolean firstVisit);
    List<QueueEntry> queue(Long practitioner, String site);
    record QueueEntry(UUID appointment, Agenda.Status status, int position, long estimatedMinutes) {}
}
