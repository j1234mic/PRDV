package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions", uniqueConstraints =
        @UniqueConstraint(name = "uk_permissions_code", columnNames = "code"))
@Getter
@Setter
public class PermissionEntity extends TimestampedEntity {

    @Column(nullable = false, length = 80)
    private String code;

    @Column(length = 40)
    private String module;

    @Column(length = 255)
    private String description;
}
