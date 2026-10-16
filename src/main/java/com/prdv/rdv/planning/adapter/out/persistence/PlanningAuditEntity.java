package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="planning_audit",indexes=@Index(name="idx_planning_audit",columnList="practitioner,occurredAt"))
public class PlanningAuditEntity {
    @Id UUID id;
    Long actor;
    @Column(nullable=false) Long practitioner;
    @Column(nullable=false,length=40) String action;
    @Column(nullable=false) Instant occurredAt;
    protected PlanningAuditEntity() {}
    PlanningAuditEntity(Long actor,Long practitioner,String action,Instant now) {
        this.id=UUID.randomUUID(); this.actor=actor; this.practitioner=practitioner; this.action=action; this.occurredAt=now;
    }
}
