package com.prdv.rdv.planning.domain;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AgendaManagementTest {
    final Instant now=Instant.parse("2026-10-16T08:00:00Z");
    Agenda.Slot slot(long hours) { return new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(hours*3600),now.plusSeconds(hours*3600+1800),Agenda.Type.NORMAL,Agenda.Audience.ALL); }
    Agenda.Intake intake() { return new Agenda.Intake("Moi","suivi",null,Map.of(),List.of(),false,"EMAIL"); }
    @Test void reschedulePreservesIdentityAndDoesNotCreateCancellation() {
        var first=slot(4); var second=slot(8);
        Agenda agenda=Agenda.empty(1L).addSlots(List.of(first,second),now).book(first.id(),2L,intake(),false,now);
        var original=agenda.appointments().get(0);
        Agenda moved=agenda.reschedule(original.id(),second.id(),now);
        assertThat(moved.appointments()).hasSize(1);
        assertThat(moved.appointment(original.id()).slotId()).isEqualTo(second.id());
        assertThat(moved.appointment(original.id()).bookedAt()).isEqualTo(original.bookedAt());
        assertThat(moved.appointment(original.id()).feeCents()).isZero();
        assertThat(moved.appointment(original.id()).cancellationReason()).isNull();
    }
    @Test void catalogueRejectsUnknownReasonAndMissingMandatoryAnswer() {
        var slot=slot(4);
        ConsultationReason reason=new ConsultationReason("suivi","Suivi",Agenda.Type.NORMAL,30,Set.of("allergies"),List.of("Ordonnance"));
        Agenda agenda=Agenda.empty(1L).addSlots(List.of(slot),now);
        agenda=agenda.withSettings(agenda.settings().reason("suivi",reason));
        Agenda configured=agenda;
        assertThatThrownBy(() -> configured.book(slot.id(),2L,intake(),false,now)).hasMessageContaining("Questionnaire");
        var correct=new Agenda.Intake("Moi","suivi",null,Map.of("allergies","Aucune"),List.of(),false,"IN_APP");
        assertThat(configured.book(slot.id(),2L,correct,false,now).appointments()).hasSize(1);
        var unknown=new Agenda.Intake("Moi","inconnu",null,Map.of(),List.of(),false,"IN_APP");
        assertThatThrownBy(() -> configured.book(slot.id(),2L,unknown,false,now)).hasMessageContaining("motif");
    }
    @Test void closureReplacesOnlyUnreferencedSlots() {
        var slot=slot(4);
        Agenda a=Agenda.empty(1L).addSlots(List.of(slot),now);
        Agenda closed=a.close("A",slot.start(),slot.end(),Agenda.Type.ABSENCE,now);
        assertThat(closed.slots()).hasSize(1);
        assertThat(closed.slots().get(0).type()).isEqualTo(Agenda.Type.ABSENCE);
        Agenda booked=a.book(slot.id(),2L,intake(),false,now);
        assertThatThrownBy(() -> booked.close("A",slot.start(),slot.end(),Agenda.Type.ABSENCE,now)).hasMessageContaining("Reporter");
    }
    @Test void cannotReduceCapacityBelowReservations() {
        var rules=new Agenda.Rules(30,Map.of(),Map.of(),0,0,60,180,2,24,10,3,0,false,0);
        var slot=slot(4);
        Agenda booked=Agenda.empty(1L).configure(rules).addSlots(List.of(slot),now)
                .book(slot.id(),2L,intake(),false,now).book(slot.id(),3L,intake(),false,now);
        assertThatThrownBy(() -> booked.configure(Agenda.Rules.defaults())).hasMessageContaining("Capacite");
    }
    @Test void temporaryOfferProtectsCapacityAndRequiresItsPatient() {
        var slot=slot(4);
        Agenda a=Agenda.empty(1L).addSlots(List.of(slot),now).book(slot.id(),2L,intake(),false,now);
        var waiter=new Agenda.Waiting(UUID.randomUUID(),3L,slot.start(),slot.end(),"A",slot.type(),false,now,true);
        a=a.join(waiter,now).cancel(a.appointments().get(0).id(),"Absent",now).proposeAutomatically(now);
        Agenda offered=a;
        WaitingOffer offer=offered.settings().offers().values().iterator().next();
        assertThat(offered.available(slot,now)).isFalse();
        assertThatThrownBy(() -> offered.acceptOffer(offer.id(),4L,intake(),now)).hasMessageContaining("introuvable");
        assertThatThrownBy(() -> offered.book(slot.id(),4L,intake(),false,now)).hasMessageContaining("indisponible");
        Agenda accepted=offered.acceptOffer(offer.id(),3L,intake(),now);
        assertThat(accepted.appointments().get(1).patientId()).isEqualTo(3L);
        assertThat(accepted.settings().offers()).isEmpty();
        assertThat(accepted.waiting()).isEmpty();
    }
    @Test void expiredOfferMovesToNextCandidateAndCannotBeAccepted() {
        var slot=slot(4);
        Agenda a=Agenda.empty(1L).addSlots(List.of(slot),now).book(slot.id(),2L,intake(),false,now);
        var first=new Agenda.Waiting(UUID.randomUUID(),3L,slot.start(),slot.end(),"A",slot.type(),false,now,true);
        var second=new Agenda.Waiting(UUID.randomUUID(),4L,slot.start(),slot.end(),"A",slot.type(),false,now.plusSeconds(1),true);
        a=a.join(first,now).join(second,now).cancel(a.appointments().get(0).id(),"Absent",now).proposeAutomatically(now);
        Agenda offered=a;
        WaitingOffer offer=a.settings().offers().values().iterator().next();
        assertThatThrownBy(() -> offered.acceptOffer(offer.id(),3L,intake(),now.plusSeconds(900))).hasMessageContaining("expiree");
        Agenda expired=a.proposeAutomatically(now.plusSeconds(900));
        assertThat(expired.settings().offers().values()).hasSize(1);
        assertThat(expired.settings().offers().values().iterator().next().patientId()).isEqualTo(4L);
    }
    @Test void templatesAreImmutableAndPreservedAcrossMutations() {
        SchedulePattern pattern=new SchedulePattern(LocalDate.of(2026,10,19),LocalDate.of(2026,10,19),ZoneOffset.UTC,
                Set.of(DayOfWeek.MONDAY),null,LocalTime.of(9,0),LocalTime.of(10,0),"A",Agenda.Type.NORMAL,Agenda.Audience.ALL,null,1);
        Agenda a=Agenda.empty(1L).withSettings(AgendaSettings.defaults().template("matin",pattern));
        assertThat(a.addSlots(List.of(slot(4)),now).settings().templates()).containsKey("matin");
        assertThatThrownBy(() -> a.settings().templates().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
