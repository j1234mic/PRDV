package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.adapter.out.persistence.security.EncryptedStringConverter;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_phone", columnNames = "phone")
})
@Getter
@Setter
public class UserEntity extends TimestampedEntity {

    @Column(length = 255)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProfileType profileType;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private AdminType adminType;

    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean mfaEnabled;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "totp_secret", length = 1024)
    private String totpSecret;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "pending_totp_secret", length = 1024)
    private String pendingTotpSecret;

    private int failedAttempts;
    private Instant lockedUntil;
    private Instant lastLoginAt;

    @Column(name = "token_version")
    private long tokenVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_direct_permissions",
            joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission_code", length = 80)
    private Set<String> directPermissions = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleEntity> roles = new HashSet<>();
}
