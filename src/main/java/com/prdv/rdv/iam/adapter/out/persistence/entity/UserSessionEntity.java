package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "user_id")
})
@Getter
@Setter
public class UserSessionEntity extends TimestampedEntity {

    @Column(nullable = false)
    private Long userId;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 100)
    private String deviceLabel;

    @Embedded
    @AttributeOverride(name = "country", column = @Column(name = "geo_country", length = 8))
    @AttributeOverride(name = "city", column = @Column(name = "geo_city", length = 120))
    @AttributeOverride(name = "latitude", column = @Column(name = "geo_latitude"))
    @AttributeOverride(name = "longitude", column = @Column(name = "geo_longitude"))
    private GeoLocationEmbeddable geoLocation;

    private boolean suspicious;
    private Instant lastSeenAt;
    private Instant revokedAt;
}
