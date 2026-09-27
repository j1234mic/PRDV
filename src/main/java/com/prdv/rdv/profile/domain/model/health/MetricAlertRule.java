package com.prdv.rdv.profile.domain.model.health;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Specification : regle de declenchement d'une alerte de sante.
 *
 * <p>Une regle decrit « quelle mesure, dans quel sens, au-dela de quel seuil,
 * avec quelle gravite ». Le moteur d'alertes
 * ({@link HealthAlertEvaluator}) les applique sans les connaitre : ajouter un
 * seuil consiste a ajouter une regle (Open/Closed), jamais a modifier le
 * moteur ni les cas d'usage.
 *
 * <p>Les regles par defaut correspondent aux seuils d'alerte usuels
 * (HAS / OMS) ; elles peuvent etre remplacees par des regles personnalisees
 * par le praticien ou par un moteur d'IA branché sur le meme port.
 */
public record MetricAlertRule(String code,
                              HealthMetric.MetricType metricType,
                              Boundary boundary,
                              BigDecimal threshold,
                              HealthAlert.Severity severity,
                              String message) {

    public enum Boundary { BELOW, ABOVE }

    public MetricAlertRule {
        if (code == null || code.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le code de regle est obligatoire");
        }
        if (metricType == null || boundary == null || threshold == null || severity == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Une regle d'alerte exige un type de mesure, un sens, un seuil et une gravite");
        }
        if (message == null || message.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le message d'alerte est obligatoire");
        }
    }

    /** La mesure satisfait-elle la regle (et doit donc declencher une alerte) ? */
    public boolean isSatisfiedBy(HealthMetric metric) {
        if (metric == null || metric.type() != metricType || metric.value() == null) {
            return false;
        }
        int comparison = metric.value().compareTo(threshold);
        return boundary == Boundary.ABOVE ? comparison > 0 : comparison < 0;
    }

    /** Regles par defaut appliquees aux mesures issues des objets connectes. */
    public static List<MetricAlertRule> defaultRules() {
        return List.of(
                new MetricAlertRule("SPO2_LOW", HealthMetric.MetricType.BLOOD_OXYGEN_SATURATION,
                        Boundary.BELOW, BigDecimal.valueOf(92), HealthAlert.Severity.CRITICAL,
                        "Saturation en oxygene inferieure a 92 % : consultez un professionnel de sante"),
                new MetricAlertRule("SYSTOLIC_HIGH", HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE,
                        Boundary.ABOVE, BigDecimal.valueOf(180), HealthAlert.Severity.CRITICAL,
                        "Tension systolique superieure a 180 mmHg"),
                new MetricAlertRule("SYSTOLIC_ELEVATED", HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE,
                        Boundary.ABOVE, BigDecimal.valueOf(140), HealthAlert.Severity.WARNING,
                        "Tension systolique superieure a 140 mmHg"),
                new MetricAlertRule("SYSTOLIC_LOW", HealthMetric.MetricType.SYSTOLIC_BLOOD_PRESSURE,
                        Boundary.BELOW, BigDecimal.valueOf(90), HealthAlert.Severity.WARNING,
                        "Tension systolique inferieure a 90 mmHg"),
                new MetricAlertRule("DIASTOLIC_HIGH", HealthMetric.MetricType.DIASTOLIC_BLOOD_PRESSURE,
                        Boundary.ABOVE, BigDecimal.valueOf(110), HealthAlert.Severity.CRITICAL,
                        "Tension diastolique superieure a 110 mmHg"),
                new MetricAlertRule("GLUCOSE_HIGH", HealthMetric.MetricType.BLOOD_GLUCOSE,
                        Boundary.ABOVE, BigDecimal.valueOf(2.50), HealthAlert.Severity.CRITICAL,
                        "Glycemie superieure a 2,50 g/L"),
                new MetricAlertRule("GLUCOSE_LOW", HealthMetric.MetricType.BLOOD_GLUCOSE,
                        Boundary.BELOW, BigDecimal.valueOf(0.60), HealthAlert.Severity.CRITICAL,
                        "Glycemie inferieure a 0,60 g/L : risque d'hypoglycemie"),
                new MetricAlertRule("HEART_RATE_HIGH", HealthMetric.MetricType.HEART_RATE,
                        Boundary.ABOVE, BigDecimal.valueOf(120), HealthAlert.Severity.WARNING,
                        "Frequence cardiaque au repos superieure a 120 bpm"),
                new MetricAlertRule("HEART_RATE_LOW", HealthMetric.MetricType.HEART_RATE,
                        Boundary.BELOW, BigDecimal.valueOf(45), HealthAlert.Severity.WARNING,
                        "Frequence cardiaque inferieure a 45 bpm"),
                new MetricAlertRule("TEMPERATURE_HIGH", HealthMetric.MetricType.BODY_TEMPERATURE,
                        Boundary.ABOVE, BigDecimal.valueOf(38.5), HealthAlert.Severity.WARNING,
                        "Temperature superieure a 38,5 degC"),
                new MetricAlertRule("RESPIRATORY_HIGH", HealthMetric.MetricType.RESPIRATORY_RATE,
                        Boundary.ABOVE, BigDecimal.valueOf(25), HealthAlert.Severity.WARNING,
                        "Frequence respiratoire superieure a 25/min"),
                new MetricAlertRule("ECG_IRREGULAR", HealthMetric.MetricType.ECG_IRREGULAR_RHYTHM,
                        Boundary.ABOVE, BigDecimal.ZERO, HealthAlert.Severity.WARNING,
                        "Rythme cardiaque irregulier detecte par l'ECG portable")
        );
    }
}
