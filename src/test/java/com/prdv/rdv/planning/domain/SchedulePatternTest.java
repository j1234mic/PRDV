package com.prdv.rdv.planning.domain;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SchedulePatternTest {
    @Test void generatesWeeklySlotsWithTypeDuration() {
        SchedulePattern pattern=new SchedulePattern(LocalDate.of(2026,10,19),LocalDate.of(2026,10,26),
                ZoneId.of("Indian/Antananarivo"),Set.of(DayOfWeek.MONDAY),null,
                LocalTime.of(9,0),LocalTime.of(10,0),"A",Agenda.Type.NORMAL,Agenda.Audience.ALL,null,1);
        var slots=pattern.materialize(Agenda.Rules.defaults());
        assertThat(slots).hasSize(4);
        assertThat(slots.get(0).start()).isEqualTo(Instant.parse("2026-10-19T06:00:00Z"));
    }
    @Test void monthlySkipsMissingDayRatherThanDrifting() {
        SchedulePattern pattern=new SchedulePattern(LocalDate.of(2027,1,1),LocalDate.of(2027,3,31),ZoneOffset.UTC,
                Set.of(),31,LocalTime.of(9,0),LocalTime.of(9,30),"A",Agenda.Type.NORMAL,Agenda.Audience.ALL,null,1);
        assertThat(pattern.materialize(Agenda.Rules.defaults())).hasSize(2);
    }
    @Test void daylightSavingAmbiguityIsRejected() {
        SchedulePattern pattern=new SchedulePattern(LocalDate.of(2026,10,25),LocalDate.of(2026,10,25),ZoneId.of("Europe/Paris"),
                Set.of(DayOfWeek.SUNDAY),null,LocalTime.of(2,0),LocalTime.of(3,0),"A",Agenda.Type.NORMAL,Agenda.Audience.ALL,null,1);
        assertThatThrownBy(() -> pattern.materialize(Agenda.Rules.defaults())).isInstanceOf(PlanningException.class);
    }
    @Test void pathologyDurationMultiplierAndBuffersOccupyTheWholeSlot() {
        Agenda.Rules rules=new Agenda.Rules(30,Map.of(Agenda.Type.NORMAL,20),Map.of("pathology",15),5,10,60,180,1,24,10,3,0,false,0);
        SchedulePattern pattern=new SchedulePattern(LocalDate.of(2026,10,19),LocalDate.of(2026,10,19),ZoneOffset.UTC,
                Set.of(DayOfWeek.MONDAY),null,LocalTime.of(9,0),LocalTime.of(10,0),"A",Agenda.Type.NORMAL,
                Agenda.Audience.ALL,"pathology",2);
        var slots=pattern.materialize(rules);
        assertThat(slots).hasSize(1);
        assertThat(Duration.between(slots.get(0).start(),slots.get(0).end()).toMinutes()).isEqualTo(45);
    }

}
