package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "practitioner_contracts",
        indexes = @Index(name = "idx_contract_practitioner", columnList = "practitioner_user_id"))
@Getter
@Setter
public class PractitionerContractEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long practitionerUserId;

    @Column(length = 20)
    private String version;

    @Column(length = 128)
    private String contentHash;

    private Instant acceptedAt;

    @Column(length = 64)
    private String ipAddress;
}
