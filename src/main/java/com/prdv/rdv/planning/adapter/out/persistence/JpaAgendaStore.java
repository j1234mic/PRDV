package com.prdv.rdv.planning.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.planning.application.port.output.AgendaStore;
import com.prdv.rdv.planning.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.function.UnaryOperator;

@Repository
public class JpaAgendaStore implements AgendaStore {
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
        try {
            Agenda initial=Agenda.empty(id);
            repository.saveAndFlush(new AgendaEntity(id,encode(initial)));
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
    public Agenda read(Long id) { return decode(repository.findById(id).orElseThrow(this::missing).payload); }
    @Transactional
    public Agenda update(Long id, UnaryOperator<Agenda> change) {
        coordinator.lock();
        AgendaEntity entity=repository.lockById(id).orElseThrow(this::missing);
        Agenda previous=decode(entity.payload);
        Agenda next=change.apply(previous);
        if (!id.equals(next.practitionerId())) throw new IllegalArgumentException("Identite agenda immutable");
        for (AgendaEntity otherEntity : repository.findAll()) {
            if (id.equals(otherEntity.practitionerId)) continue;
            Agenda other=decode(otherEntity.payload);
            for (Agenda.Appointment current : next.appointments()) {
                if (!current.status().active()) continue;
                Agenda.Slot candidate=next.slot(current.slotId());
                boolean conflict=other.appointments().stream().filter(a -> a.status().active() && a.patientId().equals(current.patientId()))
                        .map(a -> other.slot(a.slotId())).anyMatch(s -> candidate.start().isBefore(s.end()) && s.start().isBefore(candidate.end()));
                if(conflict) throw new PlanningException(PlanningException.Code.CONFLICT,
                        "Le patient a deja un rendez-vous chez un autre praticien sur cette plage");
            }
        }
        entity.payload=encode(next); repository.saveAndFlush(entity); journal.record(previous,next); return next;
    }
    private PlanningException missing() { return new PlanningException(PlanningException.Code.NOT_FOUND,"Agenda introuvable"); }
    private String encode(Agenda agenda) {
        try { return mapper.writeValueAsString(agenda); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Serialisation agenda",e); }
    }
    private Agenda decode(String payload) {
        try { return mapper.readValue(payload,Agenda.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Lecture agenda",e); }
    }
}
