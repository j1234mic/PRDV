package com.prdv.rdv.planning.application.service;

import com.prdv.rdv.planning.application.port.input.PlanningMaintenanceUseCase;
import com.prdv.rdv.planning.application.port.output.AgendaStore;
import java.time.Clock;
import java.util.List;

public class PlanningMaintenanceService implements PlanningMaintenanceUseCase {
    private final AgendaStore store;
    private final Clock clock;
    public PlanningMaintenanceService(AgendaStore store,Clock clock) { this.store=store; this.clock=clock; }
    public int reconcileWaitingOffers() {
        int changed=0;
        for(int page=0;;page++) {
            List<Long> ids=store.practitioners(page,100);
            for(Long id : ids) {
                var before=store.read(id);
                if(before.waiting().isEmpty() && before.settings().offers().isEmpty()) continue;
                // Re-evaluate under the repository transaction, not from the stale read above.
                var after=store.update(id,a -> a.proposeAutomatically(clock.instant()));
                if(!before.equals(after)) changed++;
            }
            if(ids.size()<100) return changed;
        }
    }
}
