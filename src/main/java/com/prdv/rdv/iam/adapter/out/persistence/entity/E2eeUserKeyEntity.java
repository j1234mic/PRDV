package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "e2ee_user_keys")
@Getter
@Setter
public class E2eeUserKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String keyId;

    @Column(length = 64)
    private String algorithm;

    @Column(nullable = false, length = 2048)
    private String publicKeyPem;

    @Column(length = 120)
    private String fingerprint;

    private boolean active;

    private Instant createdAt;

    private Instant rotatedAt;
}
