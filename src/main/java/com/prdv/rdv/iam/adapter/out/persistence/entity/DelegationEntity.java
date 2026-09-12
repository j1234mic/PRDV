package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "delegations")
@Getter
@Setter
public class DelegationEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long granterUserId;

    @Column(nullable = false)
    private Long granteeUserId;

    @ElementCollection
    @CollectionTable(name = "delegation_permissions",
            joinColumns = @JoinColumn(name = "delegation_id"))
    @Column(name = "permission_code", length = 80)
    private Set<String> permissionCodes = new HashSet<>();

    @Column(length = 255)
    private String reason;

    private Instant validFrom;
    private Instant validUntil;
    private Instant revokedAt;
}
