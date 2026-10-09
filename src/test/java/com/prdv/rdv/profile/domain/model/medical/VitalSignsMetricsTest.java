package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Une saisie manuelle de constantes alimente la meme serie temporelle que les appareils connectes. */
class VitalSignsMetricsTest {

    private static final Instant MEASURED_AT = Instant.parse("2026-09-27T08:30:00Z");

    @Test
    @DisplayName("Chaque constante saisie devient une mesure horodatee au releve, source MANUAL")
    void manualVitalSignsBecomeTimestampedMetrics() {
        VitalSigns signs = new VitalSigns(130, 80, 72, new BigDecimal("36.8"), 97, 16,
                new BodyMetrics(new BigDecimal("175"), new BigDecimal("70")), MEASURED_AT);

        List<HealthMetric> metrics = signs.toHealthMetrics(42L, "MANUAL");

        assertThat(metrics).hasSize(8);
        for (HealthMetric metric : metrics) {
            assertThat(metric.userId()).isEqualTo(42L);
            assertThat(metric.recordedAt()).isEqualTo(MEASURED_AT);
            assertThat(metric.sourceLabel()).isEqualTo("MANUAL");
            assertThat(metric.deviceId()).isNull();
        }
        Set<HealthMetric.MetricType> types = metrics.stream()
                .map(HealthMetric::type)
                .collect(Collectors.toSet());
        assertThat(types).containsExactlyInAnyOrder(
                HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE,
                HealthMetric.MetricType.DIASTOLIC_BLOOD_PRESSURE,
                HealthMetric.MetricType.HEART_RATE,
                HealthMetric.MetricType.BODY_TEMPERATURE,
                HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION,
                HealthMetric.MetricType.RESPIRATORY_RATE,
                HealthMetric.MetricType.WEIGHT,
                HealthMetric.MetricType.BODY_MASS_INDEX);
    }

    @Test
    @DisplayName("Des constantes partielles ne produisent que les mesures renseignees")
    void partialVitalSignsProduceOnlyProvidedMetrics() {
        VitalSigns signs = new VitalSigns(null, null, 64, null, null, null, null, MEASURED_AT);

        List<HealthMetric> metrics = signs.toHealthMetrics(42L, "MANUAL");

        assertThat(metrics).hasSize(1);
        assertThat(metrics.get(0).type()).isEqualTo(HealthMetric.MetricType.HEART_RATE);
        assertThat(metrics.get(0).value()).isEqualTo(new BigDecimal("64"));
    }
}
