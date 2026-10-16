package com.prdv.rdv.planning.application;

import com.prdv.rdv.planning.application.port.output.AgendaStore;
import com.prdv.rdv.planning.application.service.PlanningMaintenanceService;
import com.prdv.rdv.planning.domain.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.function.UnaryOperator;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class PlanningMaintenanceServiceTest {
    @Test void systemMaintenanceExpiresHoldsAndPassesToNextCandidate() {
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        var slot=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(14400),now.plusSeconds(16200),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        var intake=new Agenda.Intake("Moi","Suivi",null,Map.of(),List.of(),false,"IN_APP");
        Agenda agenda=Agenda.empty(1L).addSlots(List.of(slot),now).book(slot.id(),2L,intake,false,now);
        agenda=agenda.join(new Agenda.Waiting(UUID.randomUUID(),3L,slot.start(),slot.end(),"A",slot.type(),false,now,true),now)
                .join(new Agenda.Waiting(UUID.randomUUID(),4L,slot.start(),slot.end(),"A",slot.type(),false,now.plusSeconds(1),true),now)
                .cancel(agenda.appointments().get(0).id(),"Absent",now).proposeAutomatically(now);
        Agenda offered=agenda;
        AgendaStore store=mock(AgendaStore.class);
        when(store.practitioners(0,100)).thenReturn(List.of(1L));
        when(store.read(1L)).thenReturn(offered);
        when(store.update(eq(1L),any())).thenAnswer(invocation -> {
            UnaryOperator<Agenda> operation=invocation.getArgument(1);
            Agenda result=operation.apply(offered);
            assertThat(result.settings().offers().values().iterator().next().patientId()).isEqualTo(4L);
            return result;
        });
        var maintenance=new PlanningMaintenanceService(store,Clock.fixed(now.plusSeconds(900),ZoneOffset.UTC));
        assertThat(maintenance.reconcileWaitingOffers()).isEqualTo(1);
        verify(store).update(eq(1L),any());
    }
}
