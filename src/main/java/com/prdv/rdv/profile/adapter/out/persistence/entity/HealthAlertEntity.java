package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.health.MetricAlertRule;
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

/** Alerte automatique de sante declenchee par le franchissement d'un seuil. */
@Entity
@Table(name = "profile_health_alerts", indexes = {
        @Index(name = "idx_alert_user", columnList = "user_id,acknowledged")
})
@Getter
@Setter
public class HealthAlertEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private HealthMetric.MetricType metricType;

    @Column(precision = 12, scale = 3)
    private BigDecimal observedValue;

    @Column(precision = 12, scale = 3)
    private BigDecimal threshold;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MetricAlertRule.Boundary boundary;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private HealthAlert.Severity severity;

    @Column(length = 500)
    private String message;

    private Instant triggeredAt;

    private boolean acknowledged;

    private Instant acknowledgedAt;
}
