package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "establishment_memberships", indexes = {
        @Index(name = "idx_membership_establishment", columnList = "establishment_user_id"),
        @Index(name = "idx_membership_practitioner", columnList = "practitioner_user_id")
})
@Getter
@Setter
public class EstablishmentMembershipEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long establishmentUserId;

    @Column(nullable = false)
    private Long practitionerUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstablishmentMembership.MemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private EstablishmentMembership.MembershipStatus status;

    private LocalDate validFrom;
    private LocalDate validUntil;
    private Instant requestedAt;
    private Instant decidedAt;
}
