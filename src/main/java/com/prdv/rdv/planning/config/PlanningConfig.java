package com.prdv.rdv.planning.config;

import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.planning.application.port.input.PlanningUseCase;
import com.prdv.rdv.planning.application.port.output.*;
import com.prdv.rdv.planning.application.service.PlanningService;
import org.springframework.context.annotation.*;
import java.time.Clock;

@Configuration
public class PlanningConfig {
    @Bean com.prdv.rdv.planning.application.port.input.PlanningInboxUseCase planningInbox(
            PlanningMessageStore store,PlanningActor actor,Clock clock) {
        return new com.prdv.rdv.planning.application.service.PlanningInboxService(store,actor,clock);
    }
    @Bean com.prdv.rdv.planning.application.port.input.PlanningMaintenanceUseCase planningMaintenance(AgendaStore store,Clock clock) {
        return new com.prdv.rdv.planning.application.service.PlanningMaintenanceService(store,clock);
    }
    @Bean PlanningActor planningActor(SecurityContextPort security) { return new PlanningActor() {
        public Long userId() { return security.requireCurrentUserId(); }
        public java.util.Optional<Long> currentUserId() { return security.currentUserId(); }
    }; }
    @Bean PlanningUseCase planningUseCase(AgendaStore store, PlanningActor actor, Clock clock, PracticeSiteAccess sites, CalendarImportParser importer) {
        return new PlanningService(store,actor,clock,sites,importer);
    }
}
