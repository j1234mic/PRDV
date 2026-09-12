package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_refresh_family", columnList = "family"),
        @Index(name = "idx_refresh_user", columnList = "user_id")
})
@Getter
@Setter
public class RefreshTokenEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false, length = 128)
    private String family;

    private Long sessionId;
    private Instant expiresAt;
    private boolean revoked;
    private Instant revokedAt;
}
