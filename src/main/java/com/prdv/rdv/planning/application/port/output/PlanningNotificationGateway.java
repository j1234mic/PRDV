package com.prdv.rdv.planning.application.port.output;

import java.util.UUID;

/** Real transports must fail if not configured; the persisted inbox remains the source of truth. */
public interface PlanningNotificationGateway {
    void send(UUID eventId,Long recipient,String type);
}
