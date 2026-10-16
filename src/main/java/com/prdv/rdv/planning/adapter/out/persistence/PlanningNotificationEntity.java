package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Minimal metadata only: never store symptoms, questionnaire answers or email addresses here. */
@Entity
@Table(name="planning_notifications",indexes={@Index(name="idx_planning_inbox",columnList="recipient,occurredAt"),
        @Index(name="idx_planning_delivery",columnList="deliveryState,nextAttemptAt")})
public class PlanningNotificationEntity {
    @Id UUID id;
    @Column(nullable=false) Long recipient;
    @Column(nullable=false) Long practitioner;
    UUID appointment;
    UUID slot;
    @Column(nullable=false,length=40) String type;
    @Column(nullable=false) Instant occurredAt;
    Instant readAt;
    @Column(nullable=false,length=20) String deliveryState;
    int attempts;
    @Column(nullable=false) Instant nextAttemptAt;
    protected PlanningNotificationEntity() {}
    PlanningNotificationEntity(Long recipient,Long practitioner,UUID appointment,UUID slot,String type,Instant now) {
        this.id=UUID.randomUUID(); this.recipient=recipient; this.practitioner=practitioner; this.appointment=appointment;
        this.slot=slot; this.type=type; this.occurredAt=now; this.nextAttemptAt=now; this.deliveryState="PENDING";
    }
}
