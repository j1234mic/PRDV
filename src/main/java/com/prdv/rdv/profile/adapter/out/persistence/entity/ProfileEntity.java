package com.prdv.rdv.profile.adapter.out.persistence.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Super-classe JPA des agregats du contexte profils : identifiant technique
 * et horodatages.
 *
 * <p>Les horodatages sont copies depuis le domaine (qui les pilote via une
 * horloge injectable) : aucun {@code @PrePersist} ne vient les ecraser, ce
 * qui garantit la reproductibilite des tests et la coherence des evenements.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class ProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant createdAt;

    private Instant updatedAt;
}
