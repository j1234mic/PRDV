package com.prdv.rdv.planning.adapter.out.calendar;

import com.prdv.rdv.planning.application.port.output.CalendarImportParser;
import com.prdv.rdv.planning.domain.*;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.*;
import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

/** Deliberately strict ICS subset. Unsupported recurrence/TZ must not silently create wrong availability. */
@Component
public class IcsCalendarImportAdapter implements CalendarImportParser {
    private static final DateTimeFormatter UTC=DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'").withResolverStyle(ResolverStyle.STRICT);
    public List<Agenda.Slot> busySlots(String content,String site) {
        require(content!=null && content.length()<=1_000_000,Code.INVALID,"Fichier calendrier absent ou trop volumineux");
        String unfolded=content.replaceAll("\\r?\\n[ \\t]", "");
        List<Agenda.Slot> result=new ArrayList<>(); Map<String,String> event=null;
        boolean calendar=false,closed=false;
        for(String raw : unfolded.split("\\r?\\n")) {
            String line=raw.strip();
            if(line.isEmpty()) continue;
            if(line.equals("BEGIN:VCALENDAR")) { require(!calendar && !closed,Code.INVALID,"Calendrier imbrique"); calendar=true; continue; }
            require(calendar && !closed,Code.INVALID,"Structure ICS invalide");
            if(line.equals("END:VCALENDAR")) { require(event==null,Code.INVALID,"Evenement non ferme"); closed=true; continue; }
            if(line.equals("BEGIN:VEVENT")) { require(event==null,Code.INVALID,"Evenement imbrique"); event=new HashMap<>(); continue; }
            if(line.equals("END:VEVENT")) {
                require(event!=null,Code.INVALID,"Evenement absent");
                String uid=event.get("UID"); require(uid!=null && !uid.isBlank(),Code.INVALID,"UID obligatoire");
                Instant start=parse(event.get("DTSTART")),end=parse(event.get("DTEND"));
                result.add(new Agenda.Slot(UUID.nameUUIDFromBytes((site+":"+uid).getBytes(StandardCharsets.UTF_8)),site,start,end,Agenda.Type.MEETING,Agenda.Audience.ALL));
                require(result.size()<=500,Code.INVALID,"Import limite a 500 evenements"); event=null; continue;
            }
            int colon=line.indexOf(':'); require(colon>0,Code.INVALID,"Propriete ICS invalide");
            String key=line.substring(0,colon),value=line.substring(colon+1);
            require(!key.equals("BEGIN") && !key.equals("END"),Code.INVALID,"Composant ICS non pris en charge");
            if(event!=null) {
                require(!Set.of("RRULE","RDATE","EXDATE","RECURRENCE-ID","DURATION").contains(key.split(";",2)[0]),
                        Code.INVALID,"Developper les occurrences avant import (recurrence non prise en charge)");
                if(key.startsWith("DTSTART") || key.startsWith("DTEND"))
                    require(key.equals("DTSTART") || key.equals("DTEND"),Code.INVALID,"Seules les dates UTC explicites sont prises en charge");
                if(Set.of("UID","DTSTART","DTEND").contains(key)) {
                    require(!event.containsKey(key),Code.INVALID,"Propriete ICS dupliquee"); event.put(key,value);
                }
            }
        }
        require(calendar && closed && !result.isEmpty(),Code.INVALID,"Calendrier vide ou incomplet");
        return List.copyOf(result);
    }
    private Instant parse(String value) {
        require(value!=null && value.matches("[0-9]{8}T[0-9]{6}Z"),Code.INVALID,"Date UTC requise");
        try { return LocalDateTime.parse(value,UTC).toInstant(ZoneOffset.UTC); }
        catch(DateTimeException ex) { throw new PlanningException(Code.INVALID,"Date ICS invalide"); }
    }
}
