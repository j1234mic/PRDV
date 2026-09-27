package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object : taille / poids et Indice de Masse Corporelle (IMC) derive.
 *
 * <p>L'IMC est une donnee calculee, jamais saisie : elle est derivee a chaque
 * lecture pour rester coherente avec la taille et le poids
 * (pas de duplication d'etat, invariant garanti par le type).
 */
public record BodyMetrics(BigDecimal heightCm, BigDecimal weightKg) {

    private static final BigDecimal MIN_HEIGHT = BigDecimal.valueOf(30);
    private static final BigDecimal MAX_HEIGHT = BigDecimal.valueOf(250);
    private static final BigDecimal MIN_WEIGHT = BigDecimal.valueOf(1);
    private static final BigDecimal MAX_WEIGHT = BigDecimal.valueOf(500);

    public BodyMetrics {
        requireInRange(heightCm, MIN_HEIGHT, MAX_HEIGHT, "taille (cm)");
        requireInRange(weightKg, MIN_WEIGHT, MAX_WEIGHT, "poids (kg)");
        heightCm = heightCm.setScale(1, RoundingMode.HALF_UP);
        weightKg = weightKg.setScale(1, RoundingMode.HALF_UP);
    }

    public static BodyMetrics of(double heightCm, double weightKg) {
        return new BodyMetrics(BigDecimal.valueOf(heightCm), BigDecimal.valueOf(weightKg));
    }

    /** IMC = poids (kg) / taille (m)^2, arrondi a une decimale. */
    public BigDecimal bmi() {
        BigDecimal meters = heightCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return weightKg.divide(meters.multiply(meters), 1, RoundingMode.HALF_UP);
    }

    /** Interpretation OMS de l'IMC. */
    public BmiCategory category() {
        BigDecimal bmi = bmi();
        if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0) {
            return BmiCategory.UNDERWEIGHT;
        }
        if (bmi.compareTo(BigDecimal.valueOf(25)) < 0) {
            return BmiCategory.NORMAL;
        }
        if (bmi.compareTo(BigDecimal.valueOf(30)) < 0) {
            return BmiCategory.OVERWEIGHT;
        }
        if (bmi.compareTo(BigDecimal.valueOf(35)) < 0) {
            return BmiCategory.OBESE_CLASS_I;
        }
        if (bmi.compareTo(BigDecimal.valueOf(40)) < 0) {
            return BmiCategory.OBESE_CLASS_II;
        }
        return BmiCategory.OBESE_CLASS_III;
    }

    public enum BmiCategory {
        UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE_CLASS_I, OBESE_CLASS_II, OBESE_CLASS_III
    }

    private static void requireInRange(BigDecimal value, BigDecimal min, BigDecimal max, String label) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Valeur de " + label + " hors limites physiologiques : " + value);
        }
    }
}
