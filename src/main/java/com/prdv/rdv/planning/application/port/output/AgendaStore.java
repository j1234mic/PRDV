package com.prdv.rdv.planning.application.port.output;

import com.prdv.rdv.planning.domain.Agenda;
import java.util.function.UnaryOperator;

/** update must serialize writers per practitioner and roll back on any failure. */
public interface AgendaStore {
    void create(Long practitioner);
    /** Creates the practitioner's empty agenda only when it does not exist yet.
     * @return {@code true} when this call created the agenda, {@code false} if it already existed
     */
    boolean ensureExists(Long practitioner);
    java.util.List<Long> practitioners(int page,int size);
    Agenda read(Long practitioner);
    Agenda update(Long practitioner, UnaryOperator<Agenda> change);
}
