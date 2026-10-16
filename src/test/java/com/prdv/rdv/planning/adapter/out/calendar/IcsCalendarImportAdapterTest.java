package com.prdv.rdv.planning.adapter.out.calendar;

import com.prdv.rdv.planning.domain.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IcsCalendarImportAdapterTest {
    final IcsCalendarImportAdapter parser=new IcsCalendarImportAdapter();
    String file(String properties) {
        return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:test\r\n"+properties+"\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
    }
    @Test void importsBusyTimesWithoutClinicalSummaryAndWithStableIdentity() {
        var a=parser.busySlots(file("DTSTART:20261101T090000Z\r\nDTEND:20261101T100000Z\r\nSUMMARY:Secret"),"A");
        assertThat(a).hasSize(1);
        assertThat(a.get(0).type()).isEqualTo(Agenda.Type.MEETING);
        assertThat(parser.busySlots(file("DTSTART:20261101T090000Z\r\nDTEND:20261101T100000Z"),"A")).isEqualTo(a);
    }
    @Test void rejectsUnexpandedRecurrenceAndLocalTimezone() {
        assertThatThrownBy(() -> parser.busySlots(file("DTSTART:20261101T090000Z\r\nDTEND:20261101T100000Z\r\nRRULE:FREQ=WEEKLY"),"A"))
                .isInstanceOf(PlanningException.class);
        assertThatThrownBy(() -> parser.busySlots(file("DTSTART;TZID=Europe/Paris:20261101T090000\r\nDTEND:20261101T100000Z"),"A"))
                .isInstanceOf(PlanningException.class);
    }
    @Test void rejectsImpossibleDatesAndMissingCalendarEnd() {
        assertThatThrownBy(() -> parser.busySlots(file("DTSTART:20260230T090000Z\r\nDTEND:20260301T100000Z"),"A"))
                .isInstanceOf(PlanningException.class);
        assertThatThrownBy(() -> parser.busySlots("BEGIN:VCALENDAR\n","A")).isInstanceOf(PlanningException.class);
    }
}
