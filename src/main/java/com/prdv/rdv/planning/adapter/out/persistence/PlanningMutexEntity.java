package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.persistence.*;

/** Database-wide write coordinator. Deliberately conservative until a normalized booking index is introduced. */
@Entity
@Table(name="planning_write_mutex")
public class PlanningMutexEntity {
    @Id Long id;
    protected PlanningMutexEntity() {}
    PlanningMutexEntity(Long id) { this.id=id; }
}
