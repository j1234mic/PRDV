package com.prdv.rdv.planning.application;

import com.prdv.rdv.planning.application.port.output.AgendaStore;
import com.prdv.rdv.planning.application.service.PlanningService;
import com.prdv.rdv.planning.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import static org.assertj.core.api.Assertions.*;

class PlanningServiceTest {
    Instant now=Instant.parse("2026-10-16T08:00:00Z");
    Agenda.Slot slot=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(7200),now.plusSeconds(9000),Agenda.Type.NORMAL,Agenda.Audience.ALL);
    Agenda.Intake intake=new Agenda.Intake("Moi","Suivi",null,Map.of(),List.of(),false,"EMAIL");
    class Store implements AgendaStore {
        Agenda state=Agenda.empty(1L).addSlots(List.of(slot),now).book(slot.id(),2L,intake,false,now);
        public List<Long> practitioners(int page,int size) { return page==0 ? List.of(1L) : List.of(); }
        public void create(Long id) { state=Agenda.empty(id); }
        public Agenda read(Long id) { return state; }
        public Agenda update(Long id, UnaryOperator<Agenda> fn) { state=fn.apply(state); return state; }
    }
    @Test void patientCannotReadOrCancelAnotherPatientsAppointment() {
        Store store=new Store();
        var service=new PlanningService(store,() -> 3L,Clock.fixed(now,ZoneOffset.UTC),(id,site) -> {},(content,site) -> List.of());
        assertThat(service.appointments(1L)).isEmpty();
        UUID id=store.state.appointments().get(0).id();
        assertThatThrownBy(() -> service.cancel(1L,id,"Intrusion")).hasMessageContaining("refuse");
        assertThat(store.state.appointment(id).status()).isEqualTo(Agenda.Status.CONFIRMED);
    }
    @Test void failedRescheduleDoesNotCancelOriginal() {
        Store store=new Store();
        var service=new PlanningService(store,() -> 2L,Clock.fixed(now,ZoneOffset.UTC),(id,site) -> {},(content,site) -> List.of());
        UUID id=store.state.appointments().get(0).id();
        assertThatThrownBy(() -> service.reschedule(1L,id,UUID.randomUUID(),false)).isInstanceOf(PlanningException.class);
        assertThat(store.state.appointment(id).status()).isEqualTo(Agenda.Status.CONFIRMED);
    }
    @Test void patientCannotCallThemselves() {
        Store store=new Store();
        var service=new PlanningService(store,() -> 2L,Clock.fixed(now,ZoneOffset.UTC),(id,site) -> {},(content,site) -> List.of());
        assertThatThrownBy(() -> service.transition(1L,store.state.appointments().get(0).id(),Agenda.Status.CALLED))
                .hasMessageContaining("praticien");
    }
}
