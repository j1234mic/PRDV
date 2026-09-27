package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.health.HealthMetric;

import java.time.Instant;
import java.util.List;

/**
 * Persistance des mesures de sante (forte volumetrie : les requetes sont
 * bornees dans le temps et en nombre de lignes).
 */
public interface HealthMetricRepository {

    List<HealthMetric> saveAll(List<HealthMetric> metrics);

    List<HealthMetric> find(Long userId, HealthMetric.MetricType type, Instant from, Instant to, int limit);

    void deleteByUserId(Long userId);
}
