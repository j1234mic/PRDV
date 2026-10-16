package com.prdv.rdv.planning.adapter.out.persistence;

import com.prdv.rdv.planning.application.port.output.PlanningActor;
import com.prdv.rdv.planning.domain.Agenda;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/** Audit and outbox participate in the SAME transaction as the aggregate. */
@Component
public class PlanningJournal {
    private final PlanningNotificationRepository notifications;
    private final PlanningAuditRepository audits;
    private final PlanningActor actor;
    private final Clock clock;
    public PlanningJournal(PlanningNotificationRepository notifications,PlanningAuditRepository audits,PlanningActor actor,Clock clock) {
        this.notifications=notifications; this.audits=audits; this.actor=actor; this.clock=clock;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(Agenda previous,Agenda next) {
        if(next.equals(previous)) return;
        Instant now=clock.instant();
        audits.save(new PlanningAuditEntity(actor.currentUserId().orElse(null),next.practitionerId(),previous==null ? "AGENDA_OPENED" : "AGENDA_UPDATED",now));
        for(Agenda.Appointment current : next.appointments()) {
            Agenda.Appointment old=previous==null ? null : previous.appointments().stream().filter(a -> a.id().equals(current.id())).findFirst().orElse(null);
            String type=old==null ? "BOOKED" : !old.slotId().equals(current.slotId()) ? "RESCHEDULED"
                    : old.status()!=current.status() ? current.status().name() : null;
            if(type==null) continue;
            notify(current.patientId(),next.practitionerId(),current.id(),current.slotId(),type,now,"EMAIL".equals(current.intake().communication()));
            if(!current.patientId().equals(next.practitionerId()))
                notify(next.practitionerId(),next.practitionerId(),current.id(),current.slotId(),type,now);
        }
        next.settings().offers().values().stream()
                .filter(o -> previous==null || !previous.settings().offers().containsKey(o.id()))
                .forEach(o -> notify(o.patientId(),next.practitionerId(),null,o.slotId(),"WAITING_OFFER",now,
                        next.waiting().stream().anyMatch(w -> w.id().equals(o.waitingId()) && "EMAIL".equals(w.communication()))));
        for(Agenda.Slot slot : next.slots()) {
            if(!next.available(slot,now)) continue;
            boolean alreadyAvailable=previous!=null && previous.slots().stream().anyMatch(s -> s.id().equals(slot.id()) && previous.available(s,now));
            if(alreadyAvailable) continue;
            for(Agenda.Waiting candidate : next.candidates(slot.id()))
                notify(candidate.patientId(),next.practitionerId(),null,slot.id(),"SLOT_AVAILABLE",now,"EMAIL".equals(candidate.communication()));
        }
        if(previous!=null) next.settings().delays().forEach((site,minutes) -> {
            if(Objects.equals(previous.settings().delays().get(site),minutes)) return;
            next.appointments().stream().filter(a -> a.status().active() && next.slot(a.slotId()).site().equals(site)
                    && next.slot(a.slotId()).end().isAfter(now)).map(Agenda.Appointment::patientId).distinct()
                    .forEach(patient -> notify(patient,next.practitionerId(),null,null,"PRACTITIONER_DELAY",now));
        });
    }
    private void notify(Long user,Long practitioner,UUID appointment,UUID slot,String type,Instant now) {
        notify(user,practitioner,appointment,slot,type,now,false);
    }
    private void notify(Long user,Long practitioner,UUID appointment,UUID slot,String type,Instant now,boolean email) {
        PlanningNotificationEntity message=new PlanningNotificationEntity(user,practitioner,appointment,slot,type,now);
        if(!email) message.deliveryState="IN_APP";
        notifications.save(message);
    }
}
