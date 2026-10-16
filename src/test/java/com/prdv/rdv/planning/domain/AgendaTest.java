package com.prdv.rdv.planning.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AgendaTest {
    private final Instant now=Instant.parse("2026-10-16T08:00:00Z");
    private Agenda.Slot slot(Agenda.Type type) {
        return new Agenda.Slot(UUID.randomUUID(),"cabinet-a",now.plusSeconds(7200),now.plusSeconds(9000),type,Agenda.Audience.ALL);
    }
    private Agenda.Intake intake(boolean consent) {
        return new Agenda.Intake("Moi","Suivi",null,Map.of(),List.of(),consent,"EMAIL");
    }
    @Test void immutableAndCapacityProtected() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda original=Agenda.empty(1L).addSlots(List.of(s),now);
        Agenda booked=original.book(s.id(),2L,intake(false),false,now);
        assertThat(original.appointments()).isEmpty();
        assertThat(booked.available(s,now)).isFalse();
        assertThatThrownBy(() -> booked.book(s.id(),3L,intake(false),false,now)).isInstanceOf(PlanningException.class);
        assertThatThrownBy(() -> booked.slots().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
    @ParameterizedTest @EnumSource(value=Agenda.Type.class,names={"MEETING","ADMINISTRATIVE","ABSENCE"})
    void blockedSlotsCannotBeBooked(Agenda.Type type) {
        Agenda.Slot s=slot(type);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now);
        assertThatThrownBy(() -> a.book(s.id(),2L,intake(false),true,now)).isInstanceOf(PlanningException.class);
    }
    @Test void teleconsultationRequiresConsent() {
        Agenda.Slot s=slot(Agenda.Type.TELECONSULTATION);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now);
        assertThatThrownBy(() -> a.book(s.id(),2L,intake(false),true,now)).hasMessageContaining("Consentement");
        assertThat(a.book(s.id(),2L,intake(true),true,now).appointments()).hasSize(1);
    }
    @Test void cancellationReleasesCapacityAndPreservesReason() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now).book(s.id(),2L,intake(false),false,now);
        Agenda cancelled=a.cancel(a.appointments().get(0).id(),"Indisponible",now);
        assertThat(cancelled.available(s,now)).isTrue();
        assertThat(cancelled.appointments().get(0).cancellationReason()).isEqualTo("Indisponible");
        assertThatThrownBy(() -> cancelled.cancel(a.appointments().get(0).id(),"Bis",now)).isInstanceOf(PlanningException.class);
    }
    @Test void overlappingBatchIsRejectedAtomically() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda.Slot duplicate=new Agenda.Slot(UUID.randomUUID(),"cabinet-b",s.start(),s.end(),s.type(),s.audience());
        Agenda a=Agenda.empty(1L);
        assertThatThrownBy(() -> a.addSlots(List.of(s,duplicate),now)).hasMessageContaining("Chevauchement");
        assertThat(a.slots()).isEmpty();
    }
    @Test void halfOpenIntervalsAllowAdjacentSlots() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda.Slot next=new Agenda.Slot(UUID.randomUUID(),s.site(),s.end(),s.end().plusSeconds(1800),s.type(),s.audience());
        assertThat(Agenda.empty(1L).addSlots(List.of(s,next),now).slots()).hasSize(2);
    }
    @Test void minimumNoticeAndMaximumAdvance() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now);
        assertThat(a.available(s,s.start().minusSeconds(3599))).isFalse();
        assertThat(a.available(s,now.minus(Duration.ofDays(181)))).isFalse();
        assertThat(a.available(s,s.start().minusSeconds(3600))).isTrue();
    }
    @Test void queueStateMachineAndNoShowTiming() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now).book(s.id(),2L,intake(false),false,now);
        UUID id=a.appointments().get(0).id();
        assertThatThrownBy(() -> a.transition(id,Agenda.Status.COMPLETED,now)).isInstanceOf(PlanningException.class);
        assertThatThrownBy(() -> a.transition(id,Agenda.Status.NO_SHOW,now)).isInstanceOf(PlanningException.class);
        Agenda arrived=a.transition(id,Agenda.Status.CHECKED_IN,s.start());
        Agenda completed=arrived.transition(id,Agenda.Status.CALLED,s.start()).transition(id,Agenda.Status.COMPLETED,s.end());
        assertThat(completed.appointment(id).status()).isEqualTo(Agenda.Status.COMPLETED);
        assertThat(completed.appointment(id).arrivedAt()).isEqualTo(s.start());
    }
    @Test void waitingPrioritizesUrgencyThenSeniority() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now).book(s.id(),2L,intake(false),false,now);
        Agenda.Waiting older=new Agenda.Waiting(UUID.randomUUID(),3L,s.start(),s.end(),s.site(),s.type(),false,now);
        Agenda.Waiting urgent=new Agenda.Waiting(UUID.randomUUID(),4L,s.start(),s.end(),s.site(),s.type(),true,now.plusSeconds(1));
        Agenda joined=a.join(older,now).join(urgent,now);
        assertThat(joined.candidates(s.id())).containsExactly(urgent,older);
        assertThatThrownBy(() -> joined.join(older,now)).hasMessageContaining("Deja");
    }
    @Test void cannotJoinWhenMatchingAvailabilityExists() {
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).addSlots(List.of(s),now);
        Agenda.Waiting w=new Agenda.Waiting(UUID.randomUUID(),2L,s.start(),s.end(),null,s.type(),false,now);
        assertThatThrownBy(() -> a.join(w,now)).hasMessageContaining("disponible");
    }
    @Test void audienceAndManualConfirmationAreEnforced() {
        Agenda.Rules rules=new Agenda.Rules(30,Map.of(),Map.of(),0,0,60,180,1,24,10,3,1200,true,20);
        Agenda.Slot s=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(7200),now.plusSeconds(9000),
                Agenda.Type.NORMAL,Agenda.Audience.NEW_PATIENTS);
        Agenda a=Agenda.empty(1L).configure(rules).addSlots(List.of(s),now);
        Agenda.Slot knownOnly=new Agenda.Slot(UUID.randomUUID(),"A",s.start(),s.end(),s.type(),Agenda.Audience.KNOWN_PATIENTS);
        Agenda knownAgenda=Agenda.empty(1L).addSlots(List.of(knownOnly),now);
        assertThatThrownBy(() -> knownAgenda.book(knownOnly.id(),2L,intake(false),false,now)).hasMessageContaining("Public");
        Agenda booked=a.book(s.id(),2L,intake(false),true,now);
        UUID id=booked.appointments().get(0).id();
        assertThat(booked.appointment(id).status()).isEqualTo(Agenda.Status.PENDING);
        assertThat(booked.transition(id,Agenda.Status.CONFIRMED,now).appointment(id).status()).isEqualTo(Agenda.Status.CONFIRMED);
        assertThat(booked.cancel(id,"Indisponible",now).appointment(id).feeCents()).isEqualTo(1200);
        Agenda reconfigured=booked.configure(Agenda.Rules.defaults());
        assertThat(reconfigured.cancel(id,"Indisponible",now).appointment(id).feeCents()).isEqualTo(1200);
    }
    @Test void travelGapMustBeRespectedAcrossSites() {
        Agenda.Rules rules=new Agenda.Rules(30,Map.of(),Map.of(),0,0,60,180,1,24,10,3,0,false,20);
        Agenda.Slot s=slot(Agenda.Type.NORMAL);
        Agenda a=Agenda.empty(1L).configure(rules).addSlots(List.of(s),now);
        Agenda.Slot tooClose=new Agenda.Slot(UUID.randomUUID(),"B",s.end().plusSeconds(600),s.end().plusSeconds(2400),
                Agenda.Type.NORMAL,Agenda.Audience.ALL);
        assertThatThrownBy(() -> a.addSlots(List.of(tooClose),now)).hasMessageContaining("trajet");
        Agenda.Slot enoughGap=new Agenda.Slot(UUID.randomUUID(),"B",s.end().plusSeconds(1200),s.end().plusSeconds(3000),
                Agenda.Type.NORMAL,Agenda.Audience.ALL);
        assertThat(a.addSlots(List.of(enoughGap),now).slots()).hasSize(2);
    }

}
