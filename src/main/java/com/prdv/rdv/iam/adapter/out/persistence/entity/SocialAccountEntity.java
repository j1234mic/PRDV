package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.auth.SocialAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_social_provider_user",
                columnNames = {"provider", "provider_user_id"}),
        indexes = @Index(name = "idx_social_user", columnList = "user_id"))
@Getter
@Setter
public class SocialAccountEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SocialAccount.Provider provider;

    @Column(nullable = false, length = 128)
    private String providerUserId;

    @Column(length = 255)
    private String email;
}
