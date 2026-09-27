package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance du dossier medical personnel : chaque section structuree
 * (antecedents, allergies, vaccinations, traitements, chirurgies,
 * hospitalisations, handicaps) est une liste de Value Objects serialisee en
 * JSON, chargee et sauvegardee avec l'agregat.
 */
@Entity
@Table(name = "profile_medical_records", indexes = {
        @Index(name = "idx_medical_record_patient", columnList = "patient_user_id", unique = true)
})
@Getter
@Setter
public class MedicalRecordEntity extends ProfileEntity {

    @Column(name = "patient_user_id", nullable = false, unique = true)
    private Long patientUserId;

    @Column(length = 5)
    private String bloodGroup;

    @Lob
    @Convert(converter = JsonConverters.HistoryEntryList.class)
    @Column(name = "history")
    private List<MedicalRecord.MedicalHistoryEntry> history = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.FamilyHistoryList.class)
    @Column(name = "family_history")
    private List<MedicalRecord.FamilyHistoryEntry> familyHistory = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.AllergyList.class)
    @Column(name = "allergies")
    private List<MedicalRecord.Allergy> allergies = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.VaccinationList.class)
    @Column(name = "vaccinations")
    private List<MedicalRecord.Vaccination> vaccinations = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.ChronicConditionList.class)
    @Column(name = "chronic_conditions")
    private List<MedicalRecord.ChronicCondition> chronicConditions = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.TreatmentList.class)
    @Column(name = "treatments")
    private List<MedicalRecord.Treatment> treatments = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.SurgeryList.class)
    @Column(name = "surgeries")
    private List<MedicalRecord.Surgery> surgeries = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.HospitalizationList.class)
    @Column(name = "hospitalizations")
    private List<MedicalRecord.Hospitalization> hospitalizations = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.DisabilityList.class)
    @Column(name = "disabilities")
    private List<MedicalRecord.Disability> disabilities = new ArrayList<>();

    // --- Dernieres constantes vitales -------------------------------------
    private Integer systolicMmHg;

    private Integer diastolicMmHg;

    private Integer heartRateBpm;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperatureCelsius;

    private Integer oxygenSaturationPercent;

    private Integer respiratoryRatePerMin;

    @Column(precision = 5, scale = 1)
    private BigDecimal heightCm;

    @Column(precision = 5, scale = 1)
    private BigDecimal weightKg;

    private Instant vitalsMeasuredAt;

    private Instant vitalsRecordedAt;
}
