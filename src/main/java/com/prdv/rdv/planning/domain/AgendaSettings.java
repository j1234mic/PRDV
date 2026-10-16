package com.prdv.rdv.planning.domain;

import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

public record AgendaSettings(Map<String,SchedulePattern> templates, Map<String,ConsultationReason> reasons,
                             Map<String,Integer> delays, Map<UUID,WaitingOffer> offers) {
    public AgendaSettings {
        templates=Map.copyOf(templates); reasons=Map.copyOf(reasons); delays=Map.copyOf(delays); offers=offers==null ? Map.of() : Map.copyOf(offers);
        require(templates.size()<=50 && reasons.size()<=100 && delays.size()<=100,Code.INVALID,"Limite de configuration depassee");
    }
    public AgendaSettings(Map<String,SchedulePattern> templates,Map<String,ConsultationReason> reasons,Map<String,Integer> delays) {
        this(templates,reasons,delays,Map.of());
    }
    public AgendaSettings withOffers(Map<UUID,WaitingOffer> next) { return new AgendaSettings(templates,reasons,delays,next); }
    public static AgendaSettings defaults() { return new AgendaSettings(Map.of(),Map.of(),Map.of()); }
    public AgendaSettings template(String name, SchedulePattern pattern) {
        require(name!=null && name.matches("[a-zA-Z0-9_-]{1,80}"),Code.INVALID,"Nom du modele invalide");
        Map<String,SchedulePattern> next=new HashMap<>(templates);
        if(pattern==null) next.remove(name); else next.put(name,pattern);
        return new AgendaSettings(next,reasons,delays,offers);
    }
    public AgendaSettings reason(String id, ConsultationReason reason) {
        Map<String,ConsultationReason> next=new HashMap<>(reasons);
        if(reason==null) next.remove(id); else { require(id.equals(reason.id()),Code.INVALID,"Identifiant du motif incompatible"); next.put(id,reason); }
        return new AgendaSettings(templates,next,delays,offers);
    }
    public AgendaSettings delay(String site,int minutes) {
        require(site!=null && !site.isBlank() && minutes>=0 && minutes<=480,Code.INVALID,"Retard invalide");
        Map<String,Integer> next=new HashMap<>(delays); next.put(site,minutes);
        return new AgendaSettings(templates,reasons,next,offers);
    }
}
