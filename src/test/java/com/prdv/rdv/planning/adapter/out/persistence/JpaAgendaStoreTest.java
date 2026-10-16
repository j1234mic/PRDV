package com.prdv.rdv.planning.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.planning.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace=org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
@Import({PlanningJournal.class,PlanningCoordinator.class,JpaAgendaStore.class,JpaAgendaStoreTest.JsonConfig.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class JpaAgendaStoreTest {
    @org.springframework.boot.test.context.TestConfiguration static class JsonConfig {
        @Bean java.time.Clock planningClock() { return java.time.Clock.fixed(java.time.Instant.parse("2026-10-16T08:00:00Z"),java.time.ZoneOffset.UTC); }
        @Bean com.prdv.rdv.planning.application.port.output.PlanningActor actor() { return () -> 1L; }
        @Bean ObjectMapper mapper() { return new ObjectMapper().findAndRegisterModules(); }
    }
    @Autowired JpaAgendaStore store;
    @Autowired ObjectMapper mapper;
    @Autowired PlanningNotificationRepository notifications;
    @Autowired PlanningAuditRepository audits;
    @Test void roundTripAndRollback() {
        store.create(901L);
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        Agenda.Slot slot=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(7200),now.plusSeconds(9000),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        store.update(901L,a -> a.addSlots(List.of(slot),now));
        assertThat(store.read(901L).slots()).containsExactly(slot);
        assertThatThrownBy(() -> store.update(901L,a -> {
            a.book(slot.id(),2L,intake(),false,now);
            throw new PlanningException(PlanningException.Code.CONFLICT,"Echec");
        })).isInstanceOf(PlanningException.class);
        assertThat(store.read(901L).appointments()).isEmpty();
    }
    @Test void concurrentReservationsHaveOneWinner() throws Exception {
        store.create(902L);
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        Agenda.Slot slot=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(7200),now.plusSeconds(9000),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        store.update(902L,a -> a.addSlots(List.of(slot),now));
        CountDownLatch start=new CountDownLatch(1);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(long patient : new long[]{2,3}) futures.add(executor.submit(() -> {
                start.await();
                try { store.update(902L,a -> a.book(slot.id(),patient,intake(),false,now)); return true; }
                catch(PlanningException e) { assertThat(e.code()).isEqualTo(PlanningException.Code.CONFLICT); return false; }
            }));
            start.countDown();
            int winners=0;
            for(Future<Boolean> future : futures) if(future.get(15,TimeUnit.SECONDS)) winners++;
            assertThat(winners).isEqualTo(1);
            assertThat(store.read(902L).appointments()).hasSize(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void concurrentBookingsForSamePatientAcrossPractitionersHaveOneWinner() throws Exception {
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        var slot1=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(14400),now.plusSeconds(16200),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        var slot2=new Agenda.Slot(UUID.randomUUID(),"B",slot1.start(),slot1.end(),slot1.type(),slot1.audience());
        store.create(903L); store.create(904L);
        store.update(903L,a -> a.addSlots(List.of(slot1),now)); store.update(904L,a -> a.addSlots(List.of(slot2),now));
        CountDownLatch start=new CountDownLatch(1);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first=executor.submit(() -> attempt(start,903L,slot1.id(),50L,now));
            Future<Boolean> second=executor.submit(() -> attempt(start,904L,slot2.id(),50L,now));
            start.countDown();
            int winners=(first.get(15,TimeUnit.SECONDS)?1:0)+(second.get(15,TimeUnit.SECONDS)?1:0);
            assertThat(winners).isEqualTo(1);
            assertThat(store.read(903L).appointments().size()+store.read(904L).appointments().size()).isEqualTo(1);
        } finally { executor.shutdownNow(); }
    }
    private boolean attempt(CountDownLatch start,Long practitioner,UUID slot,Long patient,Instant now) throws InterruptedException {
        start.await();
        try { store.update(practitioner,a -> a.book(slot,patient,intake(),false,now)); return true; }
        catch(PlanningException ex) { assertThat(ex.code()).isEqualTo(PlanningException.Code.CONFLICT); return false; }
    }
    @Test void failedMutationLeavesNoNotificationOrAuditAndReleaseCreatesOfferAtomically() {
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        store.create(905L);
        var slot=new Agenda.Slot(UUID.randomUUID(),"C",now.plusSeconds(21600),now.plusSeconds(23400),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        store.update(905L,a -> a.addSlots(List.of(slot),now));
        Agenda booked=store.update(905L,a -> a.book(slot.id(),60L,intake(),false,now));
        long notificationCount=notifications.count(),auditCount=audits.count();
        assertThatThrownBy(() -> store.update(905L,a -> a.book(slot.id(),61L,intake(),false,now))).isInstanceOf(PlanningException.class);
        assertThat(notifications.count()).isEqualTo(notificationCount); assertThat(audits.count()).isEqualTo(auditCount);
        var waiter=new Agenda.Waiting(UUID.randomUUID(),61L,slot.start(),slot.end(),"C",slot.type(),false,now,true);
        store.update(905L,a -> a.join(waiter,now));
        var released=store.update(905L,a -> a.cancel(booked.appointments().get(0).id(),"Absent",now).proposeAutomatically(now));
        assertThat(released.settings().offers()).hasSize(1);
        assertThat(notifications.findByRecipientOrderByOccurredAtDescIdDesc(61L,org.springframework.data.domain.PageRequest.of(0,100)))
                .anyMatch(n -> n.type.equals("WAITING_OFFER"));
    }
    @Test void readsLegacyAggregateWithoutSettingsOrCancellationSnapshot() throws Exception {
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        var slot=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(7200),now.plusSeconds(9000),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        Agenda agenda=Agenda.empty(1L).addSlots(List.of(slot),now).book(slot.id(),2L,intake(),false,now);
        var json=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.valueToTree(agenda);
        json.remove("settings");
        ((com.fasterxml.jackson.databind.node.ObjectNode)json.get("appointments").get(0)).remove("cancellationTerms");
        Agenda restored=mapper.treeToValue(json,Agenda.class);
        assertThat(restored.settings().templates()).isEmpty();
        assertThat(restored.appointments().get(0).cancellationTerms().noticeHours()).isEqualTo(24);
    }
    private Agenda.Intake intake() { return new Agenda.Intake("Moi","Suivi",null,Map.of(),List.of(),false,"EMAIL"); }
}
