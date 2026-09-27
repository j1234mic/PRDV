package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Value Object : constantes vitales relevees a un instant donne.
 *
 * <p>Chaque mesure est verifiee dans une plage physiologiquement plausible :
 * une saisie aberrante (ex. tension a 900 mmHg) est refusee immediatement au
 * lieu de polluer le dossier medical ou de declencher de fausses alertes.
 */
public record VitalSigns(Integer systolicMmHg,
                         Integer diastolicMmHg,
                         Integer heartRateBpm,
                         BigDecimal temperatureCelsius,
                         Integer oxygenSaturationPercent,
                         Integer respiratoryRatePerMin,
                         BodyMetrics bodyMetrics,
                         Instant measuredAt) {

    public VitalSigns {
        require("tension systolique", systolicMmHg, 50, 300);
        require("tension diastolique", diastolicMmHg, 20, 200);
        require("frequence cardiaque", heartRateBpm, 20, 250);
        require("saturation en oxygene", oxygenSaturationPercent, 50, 100);
        require("frequence respiratoire", respiratoryRatePerMin, 4, 80);
        if (temperatureCelsius != null
                && (temperatureCelsius.compareTo(BigDecimal.valueOf(25)) < 0
                || temperatureCelsius.compareTo(BigDecimal.valueOf(45)) > 0)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Temperature hors limites physiologiques : " + temperatureCelsius);
        }
        if (systolicMmHg != null && diastolicMmHg != null && diastolicMmHg >= systolicMmHg) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La tension diastolique doit etre inferieure a la systolique");
        }
        if (measuredAt == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date de releve des constantes est obligatoire");
        }
    }

    /** Hypertension arterielle de grade 2 (>= 160/100 mmHg). */
    public boolean isHypertensive() {
        return systolicMmHg != null && systolicMmHg >= 160
                || diastolicMmHg != null && diastolicMmHg >= 100;
    }

    public boolean isHypotensive() {
        return systolicMmHg != null && systolicMmHg <= 90;
    }

    public boolean hasFever() {
        return temperatureCelsius != null && temperatureCelsius.compareTo(BigDecimal.valueOf(38)) >= 0;
    }

    /** SpO2 &lt; 92 % : desaturation necessitant une alerte. */
    public boolean isHypoxemic() {
        return oxygenSaturationPercent != null && oxygenSaturationPercent < 92;
    }

    private static void require(String label, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw ProfileException.of(ProfileErrorCode.BIOMETRIC_OUT_OF_RANGE,
                    label + " hors limites physiologiques (" + min + "-" + max + ") : " + value);
        }
    }
}
