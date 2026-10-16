package com.prdv.rdv.planning.domain;

import java.time.*;
import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

/** Immutable aggregate. Instants avoid DST ambiguity; all intervals are half-open. */
public record Agenda(Long practitionerId, Rules rules, List<Slot> slots,
                     List<Appointment> appointments, List<Waiting> waiting, AgendaSettings settings) {
    public Agenda {
        Objects.requireNonNull(practitionerId); Objects.requireNonNull(rules);
        settings = settings == null ? AgendaSettings.defaults() : settings;
        slots = List.copyOf(slots);
        Rules snapshotRules=rules;
        appointments = appointments.stream().map(a -> a.cancellationTerms()==null
                ? new Appointment(a.id(),a.slotId(),a.patientId(),a.intake(),a.status(),a.bookedAt(),a.arrivedAt(),
                    a.cancellationReason(),a.feeCents(),new CancellationTerms(snapshotRules.cancellationNoticeHours(),snapshotRules.lateCancellationFeeCents())) : a).toList();
        waiting = List.copyOf(waiting);
    }
    public Agenda(Long practitionerId, Rules rules, List<Slot> slots, List<Appointment> appointments, List<Waiting> waiting) {
        this(practitionerId,rules,slots,appointments,waiting,AgendaSettings.defaults());
    }
    public Agenda withSettings(AgendaSettings next) { return new Agenda(practitionerId,rules,slots,appointments,waiting,next); }
    public boolean knownPatient(Long patient) {
        return appointments.stream().anyMatch(a -> a.patientId().equals(patient) && a.status()==Status.COMPLETED);
    }
    public static Agenda empty(Long id) { return new Agenda(id, Rules.defaults(), List.of(), List.of(), List.of()); }
    public enum Type {
        NORMAL, FIRST_VISIT, FOLLOW_UP, EMERGENCY, TELECONSULTATION, HOME_VISIT,
        TECHNICAL_ACT, MEETING, ADMINISTRATIVE, ABSENCE;
        public boolean bookable() { return ordinal() < MEETING.ordinal(); }
    }
    public enum Audience { ALL, NEW_PATIENTS, KNOWN_PATIENTS }
    public enum Status { PENDING, CONFIRMED, CHECKED_IN, CALLED, COMPLETED, CANCELLED, NO_SHOW;
        public boolean active() { return this != CANCELLED && this != NO_SHOW && this != COMPLETED; }
    }
    public record Rules(int defaultMinutes, Map<Type,Integer> durations, Map<String,Integer> pathologyMinutes,
                        int preparationMinutes, int cleaningMinutes, int minimumNoticeMinutes,
                        int maximumAdvanceDays, int capacity, int cancellationNoticeHours,
                        int maximumCancellations, int noShowLimit, long lateCancellationFeeCents,
                        boolean manualConfirmation, int interSiteTravelMinutes) {
        public Rules {
            durations = Map.copyOf(durations); pathologyMinutes = Map.copyOf(pathologyMinutes);
            require(defaultMinutes > 0 && defaultMinutes <= 480 && durations.values().stream().allMatch(v -> v > 0 && v <= 480)
                    && pathologyMinutes.values().stream().allMatch(v -> v > 0 && v <= 480), Code.INVALID, "Durees invalides");
            require(preparationMinutes >= 0 && preparationMinutes <= 480 && cleaningMinutes >= 0 && cleaningMinutes <= 480 && minimumNoticeMinutes >= 0 && minimumNoticeMinutes <= 525600
                    && maximumAdvanceDays > 0 && maximumAdvanceDays <= 730 && capacity > 0 && capacity <= 10
                    && cancellationNoticeHours >= 0 && cancellationNoticeHours <= 17520 && maximumCancellations > 0 && noShowLimit > 0
                    && lateCancellationFeeCents >= 0 && interSiteTravelMinutes >= 0 && interSiteTravelMinutes <= 1440,
                    Code.INVALID, "Regles invalides");
        }
        public static Rules defaults() { return new Rules(30, Map.of(Type.FIRST_VISIT,60), Map.of(), 0,0,60,180,1,24,10,3,0,false,0); }
        public int duration(Type type, String pathology, int multiplier) {
            require(multiplier >= 1 && multiplier <= 3, Code.INVALID, "Multiplicateur attendu : 1 a 3");
            return pathologyMinutes.getOrDefault(pathology == null ? "" : pathology,
                    durations.getOrDefault(type, defaultMinutes)) * multiplier;
        }
    }
    public record Slot(UUID id, String site, Instant start, Instant end, Type type, Audience audience) {
        public Slot {
            Objects.requireNonNull(id); Objects.requireNonNull(type); Objects.requireNonNull(audience);
            require(site != null && !site.isBlank() && site.length() <= 200 && start != null && end != null
                    && start.isBefore(end), Code.INVALID, "Creneau invalide");
        }
    }
    public record Intake(String beneficiary, String reason, String symptoms, Map<String,String> questionnaire,
                         List<String> documents, boolean teleconsultationConsent, String communication) {
        public Intake {
            require(beneficiary != null && !beneficiary.isBlank() && reason != null && !reason.isBlank()
                    && beneficiary.length() <= 200 && reason.length() <= 1000
                    && (symptoms == null || symptoms.length() <= 4000), Code.INVALID, "Informations patient invalides");
            questionnaire = Map.copyOf(questionnaire); documents = List.copyOf(documents);
            require(questionnaire.size() <= 50 && documents.size() <= 30 && questionnaire.entrySet().stream().allMatch(e -> e.getKey().length()<=200 && e.getValue().length()<=4000)
                    && documents.stream().allMatch(d -> d.length()<=500) && (communication==null || Set.of("EMAIL","IN_APP","SMS","PUSH").contains(communication)), Code.INVALID, "Questionnaire trop volumineux");
        }
    }
    public record CancellationTerms(int noticeHours,long feeCents) {}
    public record Appointment(UUID id, UUID slotId, Long patientId, Intake intake, Status status,
                              Instant bookedAt, Instant arrivedAt, String cancellationReason, long feeCents, CancellationTerms cancellationTerms) {
        public Appointment status(Status next, Instant arrival, String reason, long fee) {
            return new Appointment(id,slotId,patientId,intake,next,bookedAt,arrival,reason,fee,cancellationTerms);
        }
    }
    public record Waiting(UUID id, Long patientId, Instant from, Instant to, String site, Type type,
                          boolean urgent, Instant joinedAt, boolean automaticOffer, String communication) {
        public Waiting(UUID id,Long patientId,Instant from,Instant to,String site,Type type,boolean urgent,Instant joinedAt) {
            this(id,patientId,from,to,site,type,urgent,joinedAt,false,"IN_APP");
        }
        public Waiting(UUID id,Long patientId,Instant from,Instant to,String site,Type type,boolean urgent,Instant joinedAt,boolean automaticOffer) {
            this(id,patientId,from,to,site,type,urgent,joinedAt,automaticOffer,"IN_APP");
        }
        public Waiting {
            communication=communication==null ? "IN_APP" : communication;
            require(Set.of("IN_APP","EMAIL").contains(communication),Code.INVALID,"Canal de liste d'attente invalide");
            require(patientId != null && from != null && to != null && from.isBefore(to) && type != null
                    && type.bookable(), Code.INVALID, "Preferences invalides");
        }
    }
    public Slot slot(UUID id) { return slots.stream().filter(s -> s.id().equals(id)).findFirst()
            .orElseThrow(() -> new PlanningException(Code.NOT_FOUND,"Creneau introuvable")); }
    public Appointment appointment(UUID id) { return appointments.stream().filter(a -> a.id().equals(id)).findFirst()
            .orElseThrow(() -> new PlanningException(Code.NOT_FOUND,"Rendez-vous introuvable")); }
    private static boolean overlaps(Instant a, Instant b, Instant c, Instant d) { return a.isBefore(d) && c.isBefore(b); }
    public Agenda configure(Rules next) {
        require(next!=null,Code.INVALID,"Regles requises");
        for(Slot slot : slots) require(appointments.stream().filter(a -> a.slotId().equals(slot.id()) && a.status().active()).count()
                + settings.offers().values().stream().filter(o -> o.slotId().equals(slot.id())).count() <= next.capacity(),Code.CONFLICT,"Capacite inferieure aux reservations existantes");
        for(int i=0;i<slots.size();i++) for(int j=i+1;j<slots.size();j++) {
            Slot a=slots.get(i), b=slots.get(j);
            long travel=a.site().equals(b.site()) ? 0 : next.interSiteTravelMinutes()*60L;
            require(!overlaps(a.start().minusSeconds(travel),a.end().plusSeconds(travel),b.start(),b.end()),
                    Code.CONFLICT,"Temps de trajet incompatible avec l'agenda existant");
        }
        return new Agenda(practitionerId,next,slots,appointments,waiting,settings);
    }
    /** Blocked intervals also model lunch, leave and exceptional closures. Travel applies across sites. */
    public Agenda addSlots(List<Slot> additions, Instant now) {
        require(additions != null && !additions.isEmpty() && additions.size() <= 500, Code.INVALID, "Lot de 1 a 500 creneaux requis");
        List<Slot> result = new ArrayList<>(slots);
        for (Slot next : additions) {
            require(next.start().isAfter(now), Code.INVALID, "Creneau passe");
            require(result.stream().noneMatch(s -> s.id().equals(next.id())), Code.CONFLICT, "Identifiant duplique");
            for (Slot existing : result) {
                long travel = existing.site().equals(next.site()) ? 0 : rules.interSiteTravelMinutes();
                require(!overlaps(existing.start().minusSeconds(travel*60), existing.end().plusSeconds(travel*60),
                        next.start(),next.end()), Code.CONFLICT,"Chevauchement ou trajet insuffisant");
            }
            result.add(next);
        }
        return new Agenda(practitionerId,rules,result,appointments,waiting,settings);
    }
    public Agenda removeSlot(UUID id) {
        slot(id);
        require(appointments.stream().noneMatch(a -> a.slotId().equals(id)) && settings.offers().values().stream().noneMatch(o -> o.slotId().equals(id)), Code.CONFLICT,
                "Un creneau reference par un rendez-vous doit etre conserve pour l'historique");
        return new Agenda(practitionerId,rules,slots.stream().filter(s -> !s.id().equals(id)).toList(),appointments,waiting,settings);
    }
    public boolean available(Slot slot, Instant now) {
        return slot.type().bookable() && !slot.start().isBefore(now.plusSeconds(rules.minimumNoticeMinutes()*60L))
                && !slot.start().isAfter(now.plusSeconds(rules.maximumAdvanceDays()*86400L))
                && appointments.stream().filter(a -> a.slotId().equals(slot.id()) && a.status().active()).count()
                    + settings.offers().values().stream().filter(o -> o.slotId().equals(slot.id()) && o.expiresAt().isAfter(now)).count()
                    < rules.capacity();
    }
    public Agenda book(UUID slotId, Long patient, Intake intake, boolean firstVisit, Instant now) {
        Slot slot = slot(slotId);
        firstVisit = !knownPatient(patient);
        if (!settings.reasons().isEmpty()) {
            ConsultationReason reason=settings.reasons().get(intake.reason());
            require(reason!=null,Code.INVALID,"Choisissez un motif du praticien");
            reason.validate(slot,intake);
        }
        require(available(slot,now), Code.CONFLICT,"Creneau indisponible");
        require(slot.audience() == Audience.ALL || (firstVisit ? slot.audience() == Audience.NEW_PATIENTS
                : slot.audience() == Audience.KNOWN_PATIENTS), Code.INVALID,"Public du creneau incompatible");
        require(slot.type() != Type.TELECONSULTATION || intake.teleconsultationConsent(), Code.INVALID,"Consentement requis");
        require(appointments.stream().filter(a -> a.patientId().equals(patient) && a.status() == Status.NO_SHOW).count()
                < rules.noShowLimit(), Code.FORBIDDEN,"Reservation suspendue : absences repetees");
        require(appointments.stream().filter(a -> a.patientId().equals(patient) && a.status() == Status.CANCELLED).count()
                < rules.maximumCancellations(), Code.FORBIDDEN,"Limite d'annulations atteinte");
        require(appointments.stream().noneMatch(a -> a.patientId().equals(patient) && a.status().active()
                && overlaps(slot(a.slotId()).start(),slot(a.slotId()).end(),slot.start(),slot.end())),
                Code.CONFLICT,"Le patient a deja un rendez-vous sur cette plage");
        List<Appointment> next = new ArrayList<>(appointments);
        next.add(new Appointment(UUID.randomUUID(),slotId,patient,intake,rules.manualConfirmation() ? Status.PENDING : Status.CONFIRMED,
                now,null,null,0,new CancellationTerms(rules.cancellationNoticeHours(),rules.lateCancellationFeeCents())));
        Map<UUID,WaitingOffer> remainingOffers=new HashMap<>(settings.offers());
        remainingOffers.values().removeIf(o -> o.patientId().equals(patient));
        return new Agenda(practitionerId,rules,slots,next,waiting.stream().filter(w -> !w.patientId().equals(patient)).toList(),settings.withOffers(remainingOffers));
    }
    public Agenda proposeAutomatically(Instant now) {
        Map<UUID,WaitingOffer> offers=new HashMap<>(settings.offers());
        offers.values().removeIf(o -> !o.expiresAt().isAfter(now)
                || waiting.stream().noneMatch(w -> w.id().equals(o.waitingId())));
        Set<UUID> expiredEntries=settings.offers().values().stream().filter(o -> !o.expiresAt().isAfter(now))
                .map(WaitingOffer::waitingId).collect(java.util.stream.Collectors.toSet());
        Agenda current=new Agenda(practitionerId,rules,slots,appointments,
                waiting.stream().filter(w -> !expiredEntries.contains(w.id())).toList(),settings.withOffers(offers));
        for(Slot slot : slots.stream().sorted(Comparator.comparing(Slot::start)).toList()) {
            if(!slot.start().minusSeconds(rules.minimumNoticeMinutes()*60L).isAfter(now.plusSeconds(1))) continue;
            for(Waiting candidate : current.candidates(slot.id())) {
                if(!current.available(slot,now)) break;
                if(!candidate.automaticOffer() || offers.values().stream().anyMatch(o -> o.patientId().equals(candidate.patientId()))) continue;
                boolean newPatient=!knownPatient(candidate.patientId());
                if(slot.audience()!=Audience.ALL && (newPatient ? slot.audience()!=Audience.NEW_PATIENTS : slot.audience()!=Audience.KNOWN_PATIENTS)) continue;
                WaitingOffer offer=new WaitingOffer(UUID.randomUUID(),candidate.id(),slot.id(),candidate.patientId(),now.plusSeconds(Math.min(900,Math.max(1,Duration.between(now,slot.start().minusSeconds(rules.minimumNoticeMinutes()*60L)).getSeconds()))));
                offers.put(offer.id(),offer); current=current.withSettings(current.settings().withOffers(offers));
            }
        }
        return current;
    }
    public Agenda acceptOffer(UUID id,Long patient,Intake intake,Instant now) {
        WaitingOffer offer=settings.offers().get(id);
        require(offer!=null && offer.patientId().equals(patient),Code.NOT_FOUND,"Proposition introuvable");
        require(offer.expiresAt().isAfter(now),Code.CONFLICT,"Proposition expiree");
        Map<UUID,WaitingOffer> offers=new HashMap<>(settings.offers()); offers.remove(id);
        return withSettings(settings.withOffers(offers)).book(offer.slotId(),patient,intake,false,now);
    }
    public Agenda declineOffer(UUID id,Long patient) {
        WaitingOffer offer=settings.offers().get(id);
        require(offer!=null && offer.patientId().equals(patient),Code.NOT_FOUND,"Proposition introuvable");
        Map<UUID,WaitingOffer> offers=new HashMap<>(settings.offers()); offers.remove(id);
        // A decline also removes this waiting entry to avoid proposing the same slot repeatedly.
        return new Agenda(practitionerId,rules,slots,appointments,waiting.stream().filter(w -> !w.id().equals(offer.waitingId())).toList(),settings.withOffers(offers));
    }
    public Agenda reschedule(UUID id, UUID target, Instant now) {
        Appointment previous=appointment(id);
        require(previous.status()==Status.PENDING || previous.status()==Status.CONFIRMED,Code.CONFLICT,"Report impossible");
        require(now.isBefore(slot(previous.slotId()).start()),Code.CONFLICT,"Rendez-vous deja commence");
        require(!previous.slotId().equals(target),Code.INVALID,"Choisir un autre creneau");
        Agenda without=new Agenda(practitionerId,rules,slots,appointments.stream().filter(a -> !a.id().equals(id)).toList(),waiting,settings);
        Agenda validated=without.book(target,previous.patientId(),previous.intake(),false,now);
        Appointment fresh=validated.appointments().get(validated.appointments().size()-1);
        Appointment moved=new Appointment(previous.id(),target,previous.patientId(),previous.intake(),fresh.status(),
                previous.bookedAt(),null,null,previous.feeCents(),previous.cancellationTerms());
        return new Agenda(practitionerId,rules,slots,appointments.stream().map(a -> a.id().equals(id) ? moved : a).toList(),waiting,settings);
    }
    public Agenda close(String site, Instant from, Instant to, Type type, Instant now) {
        require(type==Type.ABSENCE || type==Type.MEETING || type==Type.ADMINISTRATIVE,Code.INVALID,"Type de blocage invalide");
        List<Slot> affected=slots.stream().filter(s -> s.site().equals(site) && overlaps(s.start(),s.end(),from,to)).toList();
        require(appointments.stream().noneMatch(a -> affected.stream().anyMatch(s -> s.id().equals(a.slotId()))),
                Code.CONFLICT,"Reporter les rendez-vous avant de fermer la plage");
        require(settings.offers().values().stream().noneMatch(o -> affected.stream().anyMatch(s -> s.id().equals(o.slotId()))),Code.CONFLICT,"Une proposition reserve cette plage");
        Agenda cleared=new Agenda(practitionerId,rules,slots.stream().filter(s -> !affected.contains(s)).toList(),appointments,waiting,settings);
        return cleared.addSlots(List.of(new Slot(UUID.randomUUID(),site,from,to,type,Audience.ALL)),now);
    }
    public Agenda replace(Appointment appointment) {
        appointment(appointment.id());
        return new Agenda(practitionerId,rules,slots,appointments.stream().map(a -> a.id().equals(appointment.id()) ? appointment : a).toList(),waiting,settings);
    }
    public Agenda cancel(UUID id, String reason, Instant now) {
        Appointment a = appointment(id);
        require(a.status() == Status.PENDING || a.status() == Status.CONFIRMED, Code.CONFLICT,"Annulation impossible");
        require(reason != null && !reason.isBlank() && reason.length() <= 1000, Code.INVALID,"Motif requis");
        require(now.isBefore(slot(a.slotId()).start()), Code.CONFLICT,"Rendez-vous deja commence");
        CancellationTerms terms=a.cancellationTerms()==null
                ? new CancellationTerms(rules.cancellationNoticeHours(),rules.lateCancellationFeeCents()) : a.cancellationTerms();
        long fee = now.plusSeconds(terms.noticeHours()*3600L).isAfter(slot(a.slotId()).start()) ? terms.feeCents() : 0;
        return replace(a.status(Status.CANCELLED,null,reason,fee));
    }
    public Agenda transition(UUID id, Status next, Instant now) {
        Appointment a = appointment(id);
        boolean valid = switch(next) {
            case CONFIRMED -> a.status() == Status.PENDING;
            case CHECKED_IN -> a.status() == Status.CONFIRMED && !now.isBefore(slot(a.slotId()).start().minusSeconds(3600))
                    && now.isBefore(slot(a.slotId()).end());
            case CALLED -> a.status() == Status.CHECKED_IN;
            case COMPLETED -> a.status() == Status.CALLED;
            case NO_SHOW -> a.status() == Status.CONFIRMED && !now.isBefore(slot(a.slotId()).end());
            default -> false;
        };
        require(valid, Code.CONFLICT,"Transition impossible");
        return replace(a.status(next,next == Status.CHECKED_IN ? now : a.arrivedAt(),null,0));
    }
    public Agenda join(Waiting entry, Instant now) {
        require(entry.to().isAfter(now), Code.INVALID,"Preferences passees");
        require(waiting.size()<1000,Code.CONFLICT,"Liste d’attente complete");
        require(waiting.stream().noneMatch(w -> w.patientId().equals(entry.patientId())),Code.CONFLICT,"Deja inscrit");
        require(slots.stream().noneMatch(s -> matches(entry,s) && available(s,now)),Code.CONFLICT,"Un creneau est disponible");
        List<Waiting> next = new ArrayList<>(waiting); next.add(entry);
        return new Agenda(practitionerId,rules,slots,appointments,next,settings);
    }
    public boolean matches(Waiting w, Slot s) {
        return (s.audience()==Audience.ALL || (knownPatient(w.patientId())
                ? s.audience()==Audience.KNOWN_PATIENTS : s.audience()==Audience.NEW_PATIENTS))
                && !s.start().isBefore(w.from()) && !s.end().isAfter(w.to()) && w.type() == s.type()
                && (w.site() == null || w.site().equals(s.site()));
    }
    public List<Waiting> candidates(UUID slotId) {
        Slot s = slot(slotId);
        return waiting.stream().filter(w -> matches(w,s)).sorted(Comparator.comparing(Waiting::urgent).reversed()
                .thenComparing(Waiting::joinedAt).thenComparing(Waiting::id)).toList();
    }
}
