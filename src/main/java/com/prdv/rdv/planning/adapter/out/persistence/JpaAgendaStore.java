package com.prdv.rdv.planning.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.planning.application.port.output.AgendaStore;
import com.prdv.rdv.planning.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.function.UnaryOperator;

@Repository
public class JpaAgendaStore implements AgendaStore {
    private static final Logger log = LoggerFactory.getLogger(JpaAgendaStore.class);
    private final AgendaJpaRepository repository;
    private final ObjectMapper mapper;
    private final PlanningCoordinator coordinator;
    private final PlanningJournal journal;
    public JpaAgendaStore(AgendaJpaRepository repository,ObjectMapper mapper,PlanningCoordinator coordinator,PlanningJournal journal) {
        this.repository=repository; this.mapper=mapper; this.coordinator=coordinator; this.journal=journal;
    }
    @Transactional
    public void create(Long id) {
        coordinator.lock();
        if (repository.existsById(id)) throw new PlanningException(PlanningException.Code.CONFLICT,"Agenda deja ouvert");
        persistInitialAgenda(id);
    }
    /**
     * Lazily provisions an empty agenda for owner-facing reads. The global planning lock
     * makes the existence check and insert atomic across concurrent requests/instances.
     */
    @Transactional
    public boolean ensureExists(Long id) {
        coordinator.lock();
        if (repository.existsById(id)) return false;
        persistInitialAgenda(id);
        return true;
    }
    private void persistInitialAgenda(Long id) {
        try {
            Agenda initial=Agenda.empty(id);
            repository.saveAndFlush(new AgendaEntity(id,encode(id,initial)));
            journal.record(null,initial);
        }
        catch (DataIntegrityViolationException e) { throw new PlanningException(PlanningException.Code.CONFLICT,"Agenda deja ouvert"); }
    }
    @Transactional(readOnly=true)
    public java.util.List<Long> practitioners(int page,int size) {
        return repository.findAll(org.springframework.data.domain.PageRequest.of(page,size,
                org.springframework.data.domain.Sort.by("practitionerId"))).getContent().stream().map(e -> e.practitionerId).toList();
    }
    @Transactional(readOnly=true)
    public Agenda read(Long id) { return decode(id,repository.findById(id).orElseThrow(this::missing).payload); }
    @Transactional
    public Agenda update(Long id, UnaryOperator<Agenda> change) {
        coordinator.lock();
        AgendaEntity entity=repository.lockById(id).orElseThrow(this::missing);
        Agenda previous=decode(id,entity.payload);
        Agenda next=change.apply(previous);
        if (!id.equals(next.practitionerId())) throw new IllegalArgumentException("Identite agenda immutable");
        for (AgendaEntity otherEntity : repository.findAll()) {
            if (id.equals(otherEntity.practitionerId)) continue;
            // Un agenda tiers illisible ne doit pas rendre TOUTES les ecritures du planning
            // impossibles (500 systemique) : on l'ecarte et on journalise pour reparation.
            Agenda other=decodeOrNull(otherEntity.practitionerId,otherEntity.payload);
            if (other==null) continue;
            for (Agenda.Appointment current : next.appointments()) {
                if (!current.status().active()) continue;
                Agenda.Slot candidate=slotOrNull(next,current.slotId());
                // Rendez-vous orphelin (creneau supprime) : aucune plage a comparer.
                if (candidate==null) continue;
                boolean conflict=other.appointments().stream().filter(a -> a.status().active() && a.patientId().equals(current.patientId()))
                        .map(a -> slotOrNull(other,a.slotId())).filter(java.util.Objects::nonNull)
                        .anyMatch(s -> candidate.start().isBefore(s.end()) && s.start().isBefore(candidate.end()));
                if(conflict) throw new PlanningException(PlanningException.Code.CONFLICT,
                        "Le patient a deja un rendez-vous chez un autre praticien sur cette plage");
            }
        }
        entity.payload=encode(id,next); repository.saveAndFlush(entity); journal.record(previous,next); return next;
    }
    private PlanningException missing() { return new PlanningException(PlanningException.Code.NOT_FOUND,"Agenda introuvable"); }
    private static Agenda.Slot slotOrNull(Agenda agenda, UUID slot) {
        return agenda.slots().stream().filter(s -> s.id().equals(slot)).findFirst().orElse(null);
    }
    private String encode(Long id,Agenda agenda) {
        try { return mapper.writeValueAsString(agenda); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Serialisation de l'agenda "+id+" impossible",e); }
    }
    private Agenda decode(Long id,String payload) {
        try { return mapper.readValue(payload,Agenda.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Lecture de l'agenda "+id+" impossible",e); }
    }
    /** Retourne {@code null} (et journalise) si la charge utile d'un AUTRE agenda est illisible. */
    private Agenda decodeOrNull(Long id,String payload) {
        try { return mapper.readValue(payload,Agenda.class); }
        catch (JsonProcessingException e) {
            log.error("Agenda du praticien {} illisible : le controle anti double reservation "
                    + "inter-praticiens l'ignore pour cette operation", id, e);
            return null;
        }
    }
}
