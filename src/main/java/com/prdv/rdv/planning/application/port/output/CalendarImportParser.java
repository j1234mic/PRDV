package com.prdv.rdv.planning.application.port.output;

import com.prdv.rdv.planning.domain.Agenda;
import java.util.List;

public interface CalendarImportParser { List<Agenda.Slot> busySlots(String content,String site); }
