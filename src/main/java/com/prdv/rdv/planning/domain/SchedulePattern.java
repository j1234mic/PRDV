package com.prdv.rdv.planning.domain;

import java.time.*;
import java.util.*;
import static com.prdv.rdv.planning.domain.PlanningException.*;

/** Bounded weekly/monthly materialization in the practitioner's time zone. */
public record SchedulePattern(LocalDate from, LocalDate until, ZoneId zone, Set<DayOfWeek> weekdays,
                              Integer dayOfMonth, LocalTime opening, LocalTime closing, String site,
                              Agenda.Type type, Agenda.Audience audience, String pathology, int multiplier) {
    public SchedulePattern {
        require(from!=null && until!=null && !until.isBefore(from) && until.isBefore(from.plusYears(2)),Code.INVALID,"Periode invalide");
        Objects.requireNonNull(zone); weekdays=Set.copyOf(weekdays);
        require(opening!=null && closing!=null && opening.isBefore(closing),Code.INVALID,"Horaires invalides");
        require(dayOfMonth==null ? !weekdays.isEmpty() : dayOfMonth>=1 && dayOfMonth<=31,Code.INVALID,"Recurrence invalide");
    }
    public List<Agenda.Slot> materialize(Agenda.Rules rules) {
        int duration=rules.duration(type,pathology,multiplier);
        int step=duration+rules.preparationMinutes()+rules.cleaningMinutes();
        List<Agenda.Slot> slots=new ArrayList<>();
        for(LocalDate date=from; !date.isAfter(until); date=date.plusDays(1)) {
            if(dayOfMonth==null ? !weekdays.contains(date.getDayOfWeek()) : date.getDayOfMonth()!=dayOfMonth) continue;
            LocalDateTime end=date.atTime(closing);
            for(LocalDateTime cursor=date.atTime(opening); !cursor.plusMinutes(step).isAfter(end); cursor=cursor.plusMinutes(step)) {
                // Refuse nonexistent/ambiguous local times rather than silently changing a medical booking.
                require(zone.getRules().getValidOffsets(cursor).size()==1
                        && zone.getRules().getValidOffsets(cursor.plusMinutes(step)).size()==1,
                        Code.INVALID,"Heure ambigue ou inexistante (changement saisonnier)");
                slots.add(new Agenda.Slot(UUID.randomUUID(),site,cursor.atZone(zone).toInstant(),
                        cursor.plusMinutes(step).atZone(zone).toInstant(),type,audience));
                require(slots.size()<=500,Code.INVALID,"Modele limite a 500 creneaux");
            }
        }
        return List.copyOf(slots);
    }
}
