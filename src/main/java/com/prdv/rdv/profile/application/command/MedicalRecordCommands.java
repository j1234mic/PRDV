package com.prdv.rdv.profile.application.command;

import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Commandes du cas d'usage « dossier medical personnel » (module 2.1 — DMP).
 */
public final class MedicalRecordCommands {

    private MedicalRecordCommands() {
    }

    public record SetBloodGroup(String value) {
    }

    public record AddHistoryEntry(String code,
                                  String label,
                                  LocalDate diagnosedOn,
                                  MedicalRecord.HistoryStatus status,
                                  String note,
                                  String source) {
    }

    public record AddFamilyHistory(MedicalRecord.FamilyRelation relation,
                                   String condition,
                                   Integer relativeAgeAtDiagnosis,
                                   String note) {
    }

    public record DeclareAllergy(String allergen,
                                 MedicalRecord.AllergenType type,
                                 MedicalRecord.AllergySeverity severity,
                                 String reaction,
                                 LocalDate declaredOn) {
    }

    public record RecordVaccination(String vaccine,
                                    String dose,
                                    LocalDate administeredOn,
                                    LocalDate nextReminderOn,
                                    String provider,
                                    boolean mandatory) {
    }

    public record AddChronicCondition(String code,
                                      String label,
                                      LocalDate since,
                                      boolean longTermCondition,
                                      String note) {
    }

    public record StartTreatment(String drug,
                                 String dosage,
                                 String frequency,
                                 String prescriber,
                                 LocalDate startedOn,
                                 String note) {
    }

    public record EndTreatment(LocalDate endedOn) {
    }

    public record AddSurgery(String procedure,
                             LocalDate performedOn,
                             String establishment,
                             String surgeon,
                             String note) {
    }

    public record AddHospitalization(String reason,
                                     String establishment,
                                     LocalDate admittedOn,
                                     LocalDate dischargedOn,
                                     String note) {
    }

    public record DeclareDisability(MedicalRecord.DisabilityType type,
                                    Integer disabilityRatePercent,
                                    List<String> aids,
                                    String note) {
    }

    /** Releve de constantes : chaque champ est optionnel, la taille/poids alimentent l'IMC. */
    public record RecordVitalSigns(Integer systolicMmHg,
                                   Integer diastolicMmHg,
                                   Integer heartRateBpm,
                                   BigDecimal temperatureCelsius,
                                   Integer oxygenSaturationPercent,
                                   Integer respiratoryRatePerMin,
                                   BigDecimal heightCm,
                                   BigDecimal weightKg) {
    }
}
