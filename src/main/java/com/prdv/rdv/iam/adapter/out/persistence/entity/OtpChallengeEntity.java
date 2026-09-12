package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "otp_challenges", indexes = {
        @Index(name = "idx_otp_target_purpose", columnList = "target,purpose")
})
@Getter
@Setter
public class OtpChallengeEntity extends TimestampedEntity {

    @Column(nullable = false, length = 255)
    private String target;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private OtpChallenge.Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OtpChallenge.Purpose purpose;

    @Column(nullable = false, length = 128)
    private String codeHash;

    private Instant expiresAt;
    private int attempts;
    private boolean consumed;
}
