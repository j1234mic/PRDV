package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Mesure de sante horodatee (forte volumetrie) : indexee pour les requetes
 * par patient, type de mesure et fenetre de temps.
 */
@Entity
@Table(name = "profile_health_metrics", indexes = {
        @Index(name = "idx_metric_lookup", columnList = "user_id,metric_type,recorded_at"),
        @Index(name = "idx_metric_device", columnList = "device_id")
})
@Getter
@Setter
public class HealthMetricEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", length = 40)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", length = 30, nullable = false)
    private HealthMetric.MetricType metricType;

    @Column(name = "metric_value", nullable = false, precision = 12, scale = 3)
    private BigDecimal metricValue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HealthMetric.MeasurementContext context;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    private Instant importedAt;

    @Column(length = 40)
    private String sourceLabel;
}
