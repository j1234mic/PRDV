package com.prdv.rdv.planning.adapter.out.notification;

import com.prdv.rdv.planning.adapter.out.persistence.PlanningOutboxDispatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="prdv.planning.notifications.email-enabled",havingValue="true")
public class PlanningOutboxWorker {
    private final PlanningOutboxDispatcher dispatcher;
    public PlanningOutboxWorker(PlanningOutboxDispatcher dispatcher) { this.dispatcher=dispatcher; }
    @Scheduled(fixedDelayString="${prdv.planning.notifications.poll-ms:30000}")
    public void deliver() { dispatcher.deliver(); }
}
