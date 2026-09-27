package com.prdv.rdv.profile.domain.model.health;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Alertes automatiques : seuils, gravite la plus forte retenue, bornes physiologiques. */
class HealthAlertEvaluatorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);
    private static final Long PATIENT = 42L;

    private final HealthAlertEvaluator evaluator = new HealthAlertEvaluator();

    @Test
    @DisplayName("Une desaturation sous 92 % declenche une alerte critique")
    void lowOxygenSaturationRaisesCriticalAlert() {
        HealthAlert alert = evaluator.evaluate(metric(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION, 88),
                        PATIENT, CLOCK)
                .orElseThrow();

        assertThat(alert.getSeverity()).isEqualTo(HealthAlert.Severity.CRITICAL);
        assertThat(alert.getMetricType()).isEqualTo(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION);
        assertThat(alert.getObservedValue()).isEqualByComparingTo("88");
        assertThat(alert.getThreshold()).isEqualByComparingTo("92");
        assertThat(alert.getBoundary()).isEqualTo(MetricAlertRule.Boundary.BELOW);
        assertThat(alert.getTriggeredAt()).isEqualTo(CLOCK.instant());
        assertThat(alert.isAcknowledged()).isFalse();
    }

    @Test
    @DisplayName("Quand plusieurs regles correspondent, seule la plus grave remonte")
    void mostSevereRuleWins() {
        HealthAlert critical = evaluator.evaluate(metric(HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE, 185),
                        PATIENT, CLOCK)
                .orElseThrow();
        assertThat(critical.getSeverity()).isEqualTo(HealthAlert.Severity.CRITICAL);

        HealthAlert warning = evaluator.evaluate(metric(HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE, 150),
                        PATIENT, CLOCK)
                .orElseThrow();
        assertThat(warning.getSeverity()).isEqualTo(HealthAlert.Severity.WARNING);
    }

    @Test
    @DisplayName("Une mesure normale ne produit aucune alerte")
    void normalMetricRaisesNothing() {
        assertThat(evaluator.evaluate(metric(HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE, 120),
                PATIENT, CLOCK)).isEmpty();
        assertThat(evaluator.evaluate(metric(HealthMetric.MetricType.STEPS, 8_400), PATIENT, CLOCK))
                .isEmpty();
    }

    @Test
    @DisplayName("L'evaluation par lot ne remonte qu'une alerte par mesure anormale")
    void batchEvaluationKeepsOneAlertPerMetric() {
        List<HealthAlert> alerts = evaluator.evaluateAll(List.of(
                metric(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION, 88),
                metric(HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE, 120),
                metric(HealthMetric.MetricType.BLOOD_GLUCOSE, 0.45)), PATIENT, CLOCK);

        assertThat(alerts).hasSize(2)
                .extracting(HealthAlert::getMetricType)
                .containsExactly(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION,
                        HealthMetric.MetricType.BLOOD_GLUCOSE);
    }

    @Test
    @DisplayName("Le referentiel de seuils est interrogeable par type de mesure")
    void rulesAreQueryableByMetricType() {
        assertThat(evaluator.rulesFor(HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE)).hasSize(3);
        assertThat(evaluator.rulesFor(HealthMetric.MetricType.STEPS)).isEmpty();
    }

    @Test
    @DisplayName("Une mesure hors bornes physiologiques est refusee des la construction")
    void implausibleMetricIsRejected() {
        assertThatThrownBy(() -> metric(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION, 140))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.BIOMETRIC_OUT_OF_RANGE);
    }

    @Test
    @DisplayName("Une regle personnalisee remplace le referentiel par defaut")
    void customRulesReplaceDefaults() {
        HealthAlertEvaluator custom = new HealthAlertEvaluator(List.of(new MetricAlertRule(
                "STEPS_LOW", HealthMetric.MetricType.STEPS, MetricAlertRule.Boundary.BELOW,
                BigDecimal.valueOf(3_000), HealthAlert.Severity.INFO, "Activite quotidienne insuffisante")));

        assertThat(custom.evaluate(metric(HealthMetric.MetricType.STEPS, 1_200), PATIENT, CLOCK))
                .get()
                .extracting(HealthAlert::getSeverity)
                .isEqualTo(HealthAlert.Severity.INFO);
        assertThat(custom.evaluate(metric(HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION, 88), PATIENT,
                CLOCK)).isEmpty();
    }

    private static HealthMetric metric(HealthMetric.MetricType type, double value) {
        return HealthMetric.of(PATIENT, "device-1", type, BigDecimal.valueOf(value),
                HealthMetric.MeasurementContext.AT_REST, CLOCK.instant(), "Apple Health");
    }
}
