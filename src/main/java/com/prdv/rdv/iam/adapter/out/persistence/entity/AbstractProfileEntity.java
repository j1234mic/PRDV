package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Profil rattache a un utilisateur (table par type concret, cle etrangere
 * applicative {@code userId}).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractProfileEntity extends TimestampedEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
}
