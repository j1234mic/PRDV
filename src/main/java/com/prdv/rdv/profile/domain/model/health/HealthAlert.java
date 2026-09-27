package com.prdv.rdv.profile.domain.model.health;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Alerte de sante automatique : declenchee lorsqu'une mesure franchit un
 * seuil defini par une {@link MetricAlertRule} (specification).
 */
@Getter
@Setter
public class HealthAlert {

    public enum Severity { INFO, WARNING, CRITICAL }

    private String id;
    private Long userId;
    private HealthMetric.MetricType metricType;
    private BigDecimal observedValue;
    private BigDecimal threshold;
    private MetricAlertRule.Boundary boundary;
    private Severity severity;
    private String message;
    private Instant triggeredAt;
    private boolean acknowledged;
    private Instant acknowledgedAt;

    public static HealthAlert raise(Long userId, MetricAlertRule rule, HealthMetric metric, Clock clock) {
        HealthAlert alert = new HealthAlert();
        alert.id = UUID.randomUUID().toString();
        alert.userId = userId;
        alert.metricType = metric.type();
        alert.observedValue = metric.value();
        alert.threshold = rule.threshold();
        alert.boundary = rule.boundary();
        alert.severity = rule.severity();
        alert.message = rule.message();
        alert.triggeredAt = clock.instant();
        return alert;
    }

    public void acknowledge(Clock clock) {
        if (acknowledged) {
            return;
        }
        this.acknowledged = true;
        this.acknowledgedAt = clock.instant();
    }
}
