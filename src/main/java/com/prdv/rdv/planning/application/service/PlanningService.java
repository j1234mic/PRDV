package com.prdv.rdv.planning.application.service;

import com.prdv.rdv.planning.application.port.input.PlanningUseCase;
import com.prdv.rdv.planning.application.port.output.*;
import com.prdv.rdv.planning.domain.*;
import java.time.*;
import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

/** Application orchestration only; transaction and identity mechanisms stay behind ports. */
public class PlanningService implements PlanningUseCase {
    private final AgendaStore store;
    private final PlanningActor actor;
    private final Clock clock;
    private final PracticeSiteAccess sites;
    private final CalendarImportParser importer;
    public PlanningService(AgendaStore store, PlanningActor actor, Clock clock, PracticeSiteAccess sites, CalendarImportParser importer) {
        this.store=store; this.actor=actor; this.clock=clock; this.sites=sites; this.importer=importer;
    }
    private Agenda mutate(Long practitioner,java.util.function.UnaryOperator<Agenda> change) {
        return store.update(practitioner,a -> change.apply(a).proposeAutomatically(clock.instant()));
    }
    public Agenda importCalendar(String content,String site) {
        sites.requireOwned(actor.userId(),site);
        List<Agenda.Slot> imported=importer.busySlots(content,site);
        return mutate(actor.userId(),a -> {
            List<Agenda.Slot> additions=imported.stream().filter(s -> a.slots().stream().noneMatch(s::equals)).toList();
            return additions.isEmpty() ? a : a.addSlots(additions,clock.instant());
        });
    }
    public List<WaitingOffer> offers(Long practitioner) {
        return store.read(practitioner).settings().offers().values().stream().filter(o -> o.expiresAt().isAfter(clock.instant())
                && (actor.userId().equals(practitioner) || actor.userId().equals(o.patientId()))).toList();
    }
    public Agenda.Appointment acceptOffer(Long practitioner,UUID offer,Agenda.Intake intake) {
        Agenda result=mutate(practitioner,a -> a.acceptOffer(offer,actor.userId(),intake,clock.instant()));
        return result.appointments().get(result.appointments().size()-1);
    }
    public void declineOffer(Long practitioner,UUID offer) {
        mutate(practitioner,a -> a.declineOffer(offer,actor.userId()));
    }
    public AgendaSettings settings() { return store.read(actor.userId()).settings(); }
    public void saveTemplate(String name,SchedulePattern pattern) {
        sites.requireOwned(actor.userId(),pattern.site());
        mutate(actor.userId(),a -> a.withSettings(a.settings().template(name,pattern)));
    }
    public void deleteTemplate(String name) { mutate(actor.userId(),a -> a.withSettings(a.settings().template(name,null))); }
    public Agenda applyTemplate(String name,LocalDate from,LocalDate until) {
        return mutate(actor.userId(),a -> {
            SchedulePattern p=a.settings().templates().get(name);
            require(p!=null,Code.NOT_FOUND,"Modele introuvable"); sites.requireOwned(actor.userId(),p.site());
            SchedulePattern next=new SchedulePattern(from,until,p.zone(),p.weekdays(),p.dayOfMonth(),p.opening(),p.closing(),p.site(),
                    p.type(),p.audience(),p.pathology(),p.multiplier());
            return a.addSlots(next.materialize(a.rules()),clock.instant());
        });
    }
    public void saveReason(String id,ConsultationReason reason) {
        mutate(actor.userId(),a -> a.withSettings(a.settings().reason(id,reason)));
    }
    public void deleteReason(String id) { mutate(actor.userId(),a -> a.withSettings(a.settings().reason(id,null))); }
    public List<ConsultationReason> reasons(Long practitioner) {
        return store.read(practitioner).settings().reasons().values().stream().sorted(Comparator.comparing(ConsultationReason::label)).toList();
    }
    public Agenda duplicate(Instant from,Instant to,int shiftDays) {
        require(from!=null && to!=null && from.isBefore(to) && Duration.between(from,to).compareTo(Duration.ofDays(62))<=0
                && shiftDays>0 && shiftDays<=730,Code.INVALID,"Periode ou decalage invalide");
        return mutate(actor.userId(),a -> {
            List<Agenda.Slot> copies=a.slots().stream().filter(s -> !s.start().isBefore(from) && !s.end().isAfter(to))
                    .map(s -> new Agenda.Slot(UUID.randomUUID(),s.site(),s.start().plus(Duration.ofDays(shiftDays)),
                            s.end().plus(Duration.ofDays(shiftDays)),s.type(),s.audience())).toList();
            copies.forEach(s -> sites.requireOwned(actor.userId(),s.site()));
            return a.addSlots(copies,clock.instant());
        });
    }
    public Agenda close(String site,Instant from,Instant to,Agenda.Type type) {
        sites.requireOwned(actor.userId(),site);
        require(from!=null && to!=null && from.isBefore(to),Code.INVALID,"Periode invalide");
        return mutate(actor.userId(),a -> a.close(site,from,to,type,clock.instant()));
    }
    public void delay(String site,int minutes) {
        sites.requireOwned(actor.userId(),site);
        mutate(actor.userId(),a -> a.withSettings(a.settings().delay(site,minutes)));
    }
    public void removeSlot(UUID slot) { mutate(actor.userId(),a -> a.removeSlot(slot)); }
    public AppointmentDetails details(Long practitioner, UUID id) {
        Agenda a=store.read(practitioner); owner(a,id);
        Agenda.Appointment appointment=a.appointment(id);
        return new AppointmentDetails(appointment,a.slot(appointment.slotId()));
    }
    public Agenda.Rules rules() { return store.read(actor.userId()).rules(); }
    public List<Agenda.Slot> slots() { return store.read(actor.userId()).slots(); }
    public void open() { store.create(actor.userId()); }
    public Agenda configure(Agenda.Rules rules) { return mutate(actor.userId(), a -> a.configure(rules)); }
    public Agenda add(List<Agenda.Slot> slots) {
        require(slots!=null,Code.INVALID,"Creneaux requis");
        slots.forEach(s -> sites.requireOwned(actor.userId(),s.site()));
        return mutate(actor.userId(), a -> a.addSlots(slots,clock.instant())); }
    public Agenda generate(SchedulePattern pattern) {
        sites.requireOwned(actor.userId(),pattern.site());
        return mutate(actor.userId(),a -> a.addSlots(pattern.materialize(a.rules()),clock.instant()));
    }
    public List<Agenda.Slot> available(Long practitioner, Instant from, Instant to, String site) {
        require(from != null && to != null && from.isBefore(to) && Duration.between(from,to).compareTo(Duration.ofDays(62)) <= 0,
                Code.INVALID,"Fenetre de recherche maximale : 62 jours");
        Agenda a=store.read(practitioner);
        return a.slots().stream().filter(s -> !s.start().isBefore(from) && s.end().compareTo(to)<=0
                && (site==null || site.equals(s.site())) && a.available(s,clock.instant()))
                .sorted(Comparator.comparing(Agenda.Slot::start)).toList();
    }
    public Agenda.Appointment book(Long practitioner, UUID slot, Agenda.Intake intake, boolean firstVisit) {
        Long patient=actor.userId();
        Agenda result=mutate(practitioner,a -> a.book(slot,patient,intake,firstVisit,clock.instant()));
        return result.appointments().get(result.appointments().size()-1);
    }
    private void owner(Agenda a, UUID appointment) {
        require(actor.userId().equals(a.practitionerId()) || actor.userId().equals(a.appointment(appointment).patientId()),
                Code.FORBIDDEN,"Acces au rendez-vous refuse");
    }
    public Agenda.Appointment cancel(Long practitioner, UUID id, String reason) {
        return mutate(practitioner,a -> { owner(a,id); return a.cancel(id,reason,clock.instant()); }).appointment(id);
    }
    public Agenda.Appointment reschedule(Long practitioner, UUID id, UUID target, boolean firstVisit) {
        return mutate(practitioner,a -> { owner(a,id); return a.reschedule(id,target,clock.instant()); }).appointment(id);
    }
    public Agenda.Appointment transition(Long practitioner, UUID id, Agenda.Status next) {
        return mutate(practitioner,a -> {
            if (next == Agenda.Status.CHECKED_IN) owner(a,id);
            else require(actor.userId().equals(practitioner),Code.FORBIDDEN,"Action reservee au praticien");
            return a.transition(id,next,clock.instant());
        }).appointment(id);
    }
    public List<Agenda.Appointment> appointments(Long practitioner) {
        Agenda a=store.read(practitioner);
        return a.appointments().stream().filter(p -> actor.userId().equals(practitioner) || actor.userId().equals(p.patientId())).toList();
    }
    public Agenda.Waiting join(Long practitioner, Instant from, Instant to, String site, Agenda.Type type, boolean urgent, boolean automaticOffer, String communication) {
        Agenda.Waiting w=new Agenda.Waiting(UUID.randomUUID(),actor.userId(),from,to,site,type,urgent,clock.instant(),automaticOffer,communication);
        mutate(practitioner,a -> a.join(w,clock.instant())); return w;
    }
    public List<Agenda.Waiting> candidates(UUID slot) { return store.read(actor.userId()).candidates(slot); }
    public void leave(Long practitioner) {
        mutate(practitioner,a -> new Agenda(a.practitionerId(),a.rules(),a.slots(),a.appointments(),
                a.waiting().stream().filter(w -> !w.patientId().equals(actor.userId())).toList(),a.settings()));
    }
    public Agenda.Appointment assign(UUID waiting, UUID slot, Agenda.Intake intake, boolean firstVisit) {
        Agenda result=mutate(actor.userId(),a -> {
            Agenda.Waiting w=a.candidates(slot).stream().filter(e -> e.id().equals(waiting)).findFirst()
                    .orElseThrow(() -> new PlanningException(Code.NOT_FOUND,"Candidat introuvable"));
            return a.book(slot,w.patientId(),intake,firstVisit,clock.instant());
        });
        return result.appointments().get(result.appointments().size()-1);
    }
    public List<QueueEntry> queue(Long practitioner, String site) {
        Agenda a=store.read(practitioner);
        List<Agenda.Appointment> queue=a.appointments().stream()
                .filter(p -> p.status()==Agenda.Status.CHECKED_IN || p.status()==Agenda.Status.CALLED)
                .filter(p -> site==null || site.equals(a.slot(p.slotId()).site()))
                .sorted(Comparator.comparing((Agenda.Appointment p) -> p.status()!=Agenda.Status.CALLED)
                        .thenComparing(p -> a.slot(p.slotId()).start()).thenComparing(Agenda.Appointment::arrivedAt)).toList();
        List<QueueEntry> result=new ArrayList<>();
        Map<String,Long> estimates=new HashMap<>();
        Map<String,Integer> positions=new HashMap<>();
        for (Agenda.Appointment p : queue) {
            String currentSite=a.slot(p.slotId()).site();
            int position=positions.getOrDefault(currentSite,1);
            long minutes=estimates.getOrDefault(currentSite,a.settings().delays().getOrDefault(currentSite,0).longValue());
            if (actor.userId().equals(practitioner) || actor.userId().equals(p.patientId()))
                result.add(new QueueEntry(p.id(),p.status(),position,minutes));
            long duration=Math.max(1,Duration.between(a.slot(p.slotId()).start(),a.slot(p.slotId()).end()).toMinutes());
            estimates.put(currentSite,minutes+duration); positions.put(currentSite,position+1);
        }
        return List.copyOf(result);
    }
}
