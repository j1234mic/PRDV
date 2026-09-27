package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;

import java.time.Instant;
import java.util.List;

/** Cas d'usage : consultation des mesures, graphiques d'evolution et alertes. */
public interface HealthMetricsQueryUseCase {

    List<ProfileViews.HealthMetricView> metrics(Long patientUserId,
                                                HealthMetric.MetricType type,
                                                Instant from,
                                                Instant to,
                                                int limit);

    /** Serie agregee (heure / jour / semaine) prete a tracer. */
    ProfileViews.MetricSeriesView series(Long patientUserId,
                                         HealthMetric.MetricType type,
                                         Instant from,
                                         Instant to,
                                         String bucket);

    List<ProfileViews.HealthAlertView> alerts(Long patientUserId, boolean onlyActive);

    void acknowledgeAlert(String alertId);
}
