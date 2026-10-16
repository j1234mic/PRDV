package com.prdv.rdv.planning.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name="planning_agendas")
public class AgendaEntity {
    @Id Long practitionerId;
    @Version long version;
    @Lob @Column(nullable=false, columnDefinition="LONGTEXT") String payload;
    protected AgendaEntity() {}
    AgendaEntity(Long id, String payload) { this.practitionerId=id; this.payload=payload; }
}
