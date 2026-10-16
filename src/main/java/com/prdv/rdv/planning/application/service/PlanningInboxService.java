package com.prdv.rdv.planning.application.service;

import com.prdv.rdv.planning.application.port.input.PlanningInboxUseCase;
import com.prdv.rdv.planning.application.port.output.*;
import com.prdv.rdv.planning.domain.PlanningException;
import java.time.Clock;
import java.util.*;

public class PlanningInboxService implements PlanningInboxUseCase {
    private final PlanningMessageStore store;
    private final PlanningActor actor;
    private final Clock clock;
    public PlanningInboxService(PlanningMessageStore store,PlanningActor actor,Clock clock) {
        this.store=store; this.actor=actor; this.clock=clock;
    }
    private void page(int page,int size) {
        PlanningException.require(page>=0 && page<=10000 && size>0 && size<=100,PlanningException.Code.INVALID,"Pagination invalide");
    }
    public List<Message> messages(int page,int size) { page(page,size); return store.messages(actor.userId(),page,size); }
    public void markRead(UUID id) { store.markRead(actor.userId(),id,clock.instant()); }
    public List<Audit> audit(int page,int size) { page(page,size); return store.audit(actor.userId(),page,size); }
}
