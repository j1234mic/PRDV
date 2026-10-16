package com.prdv.rdv.planning.application.port.input;

import java.time.Instant;
import java.util.*;

public interface PlanningInboxUseCase {
    List<Message> messages(int page,int size);
    void markRead(UUID id);
    List<Audit> audit(int page,int size);
    record Message(UUID id, String type, Long practitioner, UUID appointment, UUID slot, Instant occurredAt, Instant readAt) {}
    record Audit(UUID id, Long actor, Long practitioner, String action, Instant occurredAt) {}
}
