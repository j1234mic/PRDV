package com.prdv.rdv.planning.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="prdv.planning.waiting-offers.enabled",havingValue="true",matchIfMissing=true)
public class WaitingOfferWorker {
    private final com.prdv.rdv.planning.application.port.input.PlanningMaintenanceUseCase maintenance;
    public WaitingOfferWorker(com.prdv.rdv.planning.application.port.input.PlanningMaintenanceUseCase maintenance) { this.maintenance=maintenance; }
    @Scheduled(initialDelayString="${prdv.planning.waiting-offers.poll-ms:60000}",
            fixedDelayString="${prdv.planning.waiting-offers.poll-ms:60000}")
    public void expireAndPropose() { maintenance.reconcileWaitingOffers(); }
}
