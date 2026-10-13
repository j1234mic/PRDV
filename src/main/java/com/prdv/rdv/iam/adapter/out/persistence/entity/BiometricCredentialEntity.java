package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "biometric_credentials", uniqueConstraints = {
        @UniqueConstraint(name = "uk_biometric_credential_id", columnNames = "credential_id")
})
@Getter
@Setter
public class BiometricCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "credential_id", nullable = false, length = 255)
    private String credentialId;

    @Column(name = "public_key", nullable = false, length = 2048)
    private String publicKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private BiometricCredential.BiometricModality modality;

    @Column(length = 120)
    private String deviceName;

    private long signCount;

    private boolean revoked;

    private Instant createdAt;

    private Instant lastUsedAt;

    private Instant revokedAt;
}
