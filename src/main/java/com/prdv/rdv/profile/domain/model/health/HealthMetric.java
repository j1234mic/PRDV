package com.prdv.rdv.profile.domain.model.health;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Value Object / entite legere : une mesure de sante horodatee
 * (pas, frequence cardiaque, tension, glycemie, poids, SpO2, ECG...).
 *
 * <p>Chaque type de mesure porte son unite et sa plage de plausibilite : une
 * valeur physiquement impossible est refusee a la creation, ce qui protege
 * les graphiques d'evolution et le moteur d'alertes des donnees aberrantes
 * transmises par les objets connectes.
 */
public record HealthMetric(String id,
                           Long userId,
                           String deviceId,
                           MetricType type,
                           BigDecimal value,
                           MeasurementContext context,
                           Instant recordedAt,
                           Instant importedAt,
                           String sourceLabel) {

    /** Contexte de la mesure quand il influence l'interpretation (glycemie a jeun, effort...). */
    public enum MeasurementContext { UNSPECIFIED, FASTING, POST_MEAL, AT_REST, DURING_EFFORT, NIGHT }

    /**
     * Catalogue des mesures suivies : unite et bornes physiologiques.
     * Ajouter une mesure = ajouter une constante (Open/Closed), aucun autre
     * code n'est a modifier.
     */
    public enum MetricType {

        STEPS("pas", 0, 200_000),
        HEART_RATE("bpm", 20, 250),
        RESTING_HEART_RATE("bpm", 25, 150),
        SYSTOLIC_BLOOD_PRESSURE("mmHg", 50, 300),
        DIASTOLIC_BLOOD_PRESSURE("mmHg", 20, 200),
        BLOOD_GLUCOSE("g/L", 0.20, 6.00),
        WEIGHT("kg", 1, 500),
        BODY_MASS_INDEX("kg/m2", 8, 80),
        BLOOD_OXYGEN_SATURATION("%", 50, 100),
        BODY_TEMPERATURE("degC", 25, 45),
        RESPIRATORY_RATE("/min", 4, 80),
        SLEEP_DURATION("min", 0, 1_440),
        ECG_IRREGULAR_RHYTHM("evenement", 0, 1);

        private final String unit;
        private final BigDecimal minValue;
        private final BigDecimal maxValue;

        MetricType(String unit, double minValue, double maxValue) {
            this.unit = unit;
            this.minValue = BigDecimal.valueOf(minValue);
            this.maxValue = BigDecimal.valueOf(maxValue);
        }

        public String unit() {
            return unit;
        }

        public BigDecimal minValue() {
            return minValue;
        }

        public BigDecimal maxValue() {
            return maxValue;
        }

        public boolean isPlausible(BigDecimal value) {
            return value != null
                    && value.compareTo(minValue) >= 0
                    && value.compareTo(maxValue) <= 0;
        }
    }

    public HealthMetric {
        if (userId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Une mesure de sante doit etre rattachee a un patient");
        }
        if (type == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le type de mesure est obligatoire");
        }
        if (value == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "La valeur de la mesure est obligatoire");
        }
        if (!type.isPlausible(value)) {
            throw ProfileException.of(ProfileErrorCode.BIOMETRIC_OUT_OF_RANGE,
                    "Mesure " + type + " hors plage physiologique (" + type.minValue() + " a "
                            + type.maxValue() + " " + type.unit() + ") : " + value);
        }
        if (recordedAt == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "L'horodatage de la mesure est obligatoire");
        }
        id = id == null ? UUID.randomUUID().toString() : id;
        context = context == null ? MeasurementContext.UNSPECIFIED : context;
        sourceLabel = sourceLabel == null ? "MANUAL" : sourceLabel;
    }

    public static HealthMetric of(Long userId, String deviceId, MetricType type, BigDecimal value,
                                  MeasurementContext context, Instant recordedAt, String sourceLabel) {
        return new HealthMetric(null, userId, deviceId, type, value, context, recordedAt, null, sourceLabel);
    }

    /** Copie horodatee cote serveur (trace d'import) sans modifier la mesure d'origine. */
    public HealthMetric withImportTimestamp(Instant importedAt) {
        return new HealthMetric(id, userId, deviceId, type, value, context, recordedAt, importedAt, sourceLabel);
    }
}
