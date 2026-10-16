package com.prdv.rdv.planning.adapter.out.persistence;

import com.prdv.rdv.planning.application.port.output.PlanningNotificationGateway;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Component
public class PlanningOutboxDispatcher {
    private final PlanningNotificationRepository repository;
    private final PlanningNotificationGateway gateway;
    private final Clock clock;
    public PlanningOutboxDispatcher(PlanningNotificationRepository repository,PlanningNotificationGateway gateway,Clock clock) {
        this.repository=repository; this.gateway=gateway; this.clock=clock;
    }
    /** At-least-once transport. No claim of exactly-once SMTP delivery after process failure. */
    @Transactional
    public int deliver() {
        int delivered=0;
        for(PlanningNotificationEntity message : repository.due(clock.instant(),PageRequest.of(0,20))) {
            message.attempts++;
            try { gateway.send(message.id,message.recipient,message.type); message.deliveryState="SENT"; delivered++; }
            catch(RuntimeException ex) {
                if(message.attempts>=8) message.deliveryState="DEAD";
                message.nextAttemptAt=clock.instant().plusSeconds(Math.min(3600,30L << Math.min(message.attempts,7)));
            }
            repository.save(message);
        }
        return delivered;
    }
}
