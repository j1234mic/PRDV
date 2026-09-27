package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Constantes vitales et IMC : calcul et garde-fous physiologiques. */
class VitalSignsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-27T10:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("L'IMC est calcule puis interprete selon les seuils OMS")
    void bmiIsComputedAndInterpreted() {
        assertThat(BodyMetrics.of(175, 70).bmi()).isEqualByComparingTo("22.9");
        assertThat(BodyMetrics.of(175, 70).category()).isEqualTo(BodyMetrics.BmiCategory.NORMAL);
        assertThat(BodyMetrics.of(175, 50).category()).isEqualTo(BodyMetrics.BmiCategory.UNDERWEIGHT);
        assertThat(BodyMetrics.of(175, 90).category()).isEqualTo(BodyMetrics.BmiCategory.OVERWEIGHT);
        assertThat(BodyMetrics.of(175, 105).category()).isEqualTo(BodyMetrics.BmiCategory.OBESE_CLASS_I);
        assertThat(BodyMetrics.of(160, 100).category()).isEqualTo(BodyMetrics.BmiCategory.OBESE_CLASS_II);
        assertThat(BodyMetrics.of(160, 110).category()).isEqualTo(BodyMetrics.BmiCategory.OBESE_CLASS_III);
    }

    @Test
    @DisplayName("Une taille ou un poids hors limites physiologiques est refuse")
    void implausibleBodyMetricsAreRejected() {
        assertThatThrownBy(() -> BodyMetrics.of(20, 70))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> BodyMetrics.of(175, 0.4))
                .isInstanceOf(ProfileException.class);
    }

    @Test
    @DisplayName("Les constantes vitales detectent hypertension, hypotension, fievre et hypoxemie")
    void vitalSignsFlagsAreDerived() {
        VitalSigns hypertensive = new VitalSigns(165, 100, 82, null, 97, 16,
                BodyMetrics.of(175, 80), CLOCK.instant());
        assertThat(hypertensive.isHypertensive()).isTrue();
        assertThat(hypertensive.isHypotensive()).isFalse();
        assertThat(hypertensive.hasFever()).isFalse();

        VitalSigns critical = new VitalSigns(85, 55, 120, new java.math.BigDecimal("39.2"), 89, 26,
                BodyMetrics.of(165, 60), CLOCK.instant());
        assertThat(critical.isHypotensive()).isTrue();
        assertThat(critical.hasFever()).isTrue();
        assertThat(critical.isHypoxemic()).isTrue();
    }

    @Test
    @DisplayName("Le dossier medical conserve la derniere mesure de constantes avec son horodatage")
    void medicalRecordKeepsLatestVitalSigns() {
        MedicalRecord record = MedicalRecord.open(42L, CLOCK);
        record.recordVitalSigns(new VitalSigns(128, 82, 70, new java.math.BigDecimal("36.8"), 98, 14,
                BodyMetrics.of(178, 74), CLOCK.instant()), CLOCK);

        assertThat(record.getLastVitalSigns()).isNotNull();
        assertThat(record.getLastVitalSigns().recordedAt()).isEqualTo(CLOCK.instant());
        assertThat(record.getLastVitalSigns().vitalSigns().systolicMmHg()).isEqualTo(128);
        assertThat(record.getLastVitalSigns().vitalSigns().bodyMetrics().category())
                .isEqualTo(BodyMetrics.BmiCategory.NORMAL);
        assertThat(record.currentBodyMetrics()).isPresent();
    }

    @Test
    @DisplayName("Le groupe sanguin est parse (ABO + Rhesus) puis normalise")
    void bloodGroupIsParsedAndNormalized() {
        MedicalRecord record = MedicalRecord.open(42L, CLOCK);
        record.setBloodGroupValue(BloodGroup.of("a +"), CLOCK);

        assertThat(record.getBloodGroup().toString()).isEqualTo("A+");
        assertThat(record.getBloodGroup().rh()).isEqualTo(BloodGroup.RhFactor.POSITIVE);

        assertThatThrownBy(() -> BloodGroup.of("Z-"))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> BloodGroup.of("A"))
                .isInstanceOf(ProfileException.class)
                .extracting(exception -> ((ProfileException) exception).getErrorCode())
                .isEqualTo(ProfileErrorCode.VALIDATION_ERROR);
    }
}
