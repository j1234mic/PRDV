import com.prdv.rdv.planning.domain.*;
import java.time.*;
import java.util.*;

/** Dependency-free execution check; complements, never replaces, JUnit/JPA/E2E. */
public class PlanningSmoke {
    private static int checks;
    private static void check(boolean condition,String description) {
        if(!condition) throw new AssertionError(description);
        checks++;
    }
    private static void rejects(Runnable operation) {
        try { operation.run(); throw new AssertionError("Violation acceptee"); }
        catch(PlanningException expected) { checks++; }
    }
    public static void main(String[] args) {
        Instant now=Instant.parse("2026-10-16T08:00:00Z");
        var first=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(14400),now.plusSeconds(16200),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        var second=new Agenda.Slot(UUID.randomUUID(),"A",now.plusSeconds(28800),now.plusSeconds(30600),Agenda.Type.NORMAL,Agenda.Audience.ALL);
        var intake=new Agenda.Intake("Moi","suivi",null,Map.of(),List.of(),false,"IN_APP");
        Agenda original=Agenda.empty(1L).addSlots(List.of(first,second),now);
        Agenda booked=original.book(first.id(),2L,intake,false,now);
        check(original.appointments().isEmpty(),"Agregat immutable");
        rejects(() -> booked.book(first.id(),3L,intake,false,now));
        UUID appointment=booked.appointments().get(0).id();
        Agenda moved=booked.reschedule(appointment,second.id(),now);
        check(moved.appointments().size()==1 && moved.appointment(appointment).slotId().equals(second.id()),"Identite du report");
        check(moved.appointment(appointment).feeCents()==0 && moved.appointment(appointment).cancellationReason()==null,"Report sans annulation");
        Agenda cancelled=booked.cancel(appointment,"Absent",now);
        check(cancelled.available(first,now),"Liberation du creneau");
        var w1=new Agenda.Waiting(UUID.randomUUID(),3L,first.start(),first.end(),"A",first.type(),false,now,true);
        var w2=new Agenda.Waiting(UUID.randomUUID(),4L,first.start(),first.end(),"A",first.type(),false,now.plusSeconds(1),true);
        Agenda waiting=booked.join(w1,now).join(w2,now).cancel(appointment,"Absent",now).proposeAutomatically(now);
        WaitingOffer offer=waiting.settings().offers().values().iterator().next();
        check(offer.patientId().equals(3L),"Priorite anciennete");
        check(!waiting.available(first,now),"Capacite reservee temporairement");
        rejects(() -> waiting.acceptOffer(offer.id(),4L,intake,now));
        rejects(() -> waiting.acceptOffer(offer.id(),3L,intake,now.plusSeconds(900)));
        Agenda next=waiting.proposeAutomatically(now.plusSeconds(900));
        check(next.settings().offers().values().iterator().next().patientId().equals(4L),"Expiration vers le candidat suivant");
        Agenda accepted=waiting.acceptOffer(offer.id(),3L,intake,now);
        check(accepted.settings().offers().isEmpty() && accepted.appointments().size()==2,"Acceptation sans double reservation");
        Agenda closed=original.close("A",first.start(),first.end(),Agenda.Type.ABSENCE,now);
        check(closed.slots().stream().anyMatch(s -> s.type()==Agenda.Type.ABSENCE),"Fermeture exceptionnelle");
        rejects(() -> booked.close("A",first.start(),first.end(),Agenda.Type.ABSENCE,now));
        var reason=new ConsultationReason("suivi","Suivi",Agenda.Type.NORMAL,30,Set.of("question"),List.of("Ordonnance"));
        Agenda configured=original.withSettings(original.settings().reason(reason.id(),reason));
        rejects(() -> configured.book(first.id(),2L,intake,false,now));
        var answered=new Agenda.Intake("Moi","suivi",null,Map.of("question","Reponse"),List.of(),false,"IN_APP");
        check(configured.book(first.id(),2L,answered,false,now).appointments().size()==1,"Questionnaire valide");
        System.out.println("Planning smoke: "+checks+" checks passed");
    }
}
