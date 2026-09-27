package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Objet connecte appaire au compte d'un patient. */
@Entity
@Table(name = "profile_connected_devices", indexes = {
        @Index(name = "idx_device_user", columnList = "user_id"),
        @Index(name = "idx_device_external", columnList = "user_id,external_device_id")
})
@Getter
@Setter
public class ConnectedDeviceEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private ConnectedDevice.DeviceType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ConnectedDevice.Provider provider;

    @Column(length = 120)
    private String externalDeviceId;

    @Column(length = 120)
    private String label;

    @Column(length = 80)
    private String model;

    @Column(length = 40)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ConnectedDevice.Status status;

    @Column(length = 255)
    private String lastSyncError;

    private Instant connectedAt;

    private Instant lastSyncAt;

    private long syncedMetricsCount;
}
