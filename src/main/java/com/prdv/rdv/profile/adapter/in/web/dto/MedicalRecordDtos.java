package com.prdv.rdv.profile.adapter.in.web.dto;

import com.prdv.rdv.profile.application.command.MedicalRecordCommands;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DTO d'entree du dossier medical personnel. */
public final class MedicalRecordDtos {

    private MedicalRecordDtos() {
    }

    public record BloodGroupRequest(@NotBlank @Size(max = 8) String value) {

        public MedicalRecordCommands.SetBloodGroup toCommand() {
            return new MedicalRecordCommands.SetBloodGroup(value);
        }
    }

    public record HistoryEntryRequest(@Size(max = 30) String code,
                                      @NotBlank @Size(max = 200) String label,
                                      LocalDate diagnosedOn,
                                      @NotNull MedicalRecord.HistoryStatus status,
                                      @Size(max = 1000) String note,
                                      @Size(max = 120) String source) {

        public MedicalRecordCommands.AddHistoryEntry toCommand() {
            return new MedicalRecordCommands.AddHistoryEntry(code, label, diagnosedOn, status, note, source);
        }
    }

    public record FamilyHistoryRequest(@NotNull MedicalRecord.FamilyRelation relation,
                                       @NotBlank @Size(max = 200) String condition,
                                       @Min(0) @Max(130) Integer relativeAgeAtDiagnosis,
                                       @Size(max = 1000) String note) {

        public MedicalRecordCommands.AddFamilyHistory toCommand() {
            return new MedicalRecordCommands.AddFamilyHistory(relation, condition, relativeAgeAtDiagnosis,
                    note);
        }
    }

    public record AllergyRequest(@NotBlank @Size(max = 200) String allergen,
                                 @NotNull MedicalRecord.AllergenType type,
                                 @NotNull MedicalRecord.AllergySeverity severity,
                                 @NotBlank @Size(max = 255) String reaction,
                                 LocalDate declaredOn) {

        public MedicalRecordCommands.DeclareAllergy toCommand() {
            return new MedicalRecordCommands.DeclareAllergy(allergen, type, severity, reaction, declaredOn);
        }
    }

    public record VaccinationRequest(@NotBlank @Size(max = 150) String vaccine,
                                     @Size(max = 50) String dose,
                                     LocalDate administeredOn,
                                     LocalDate nextReminderOn,
                                     @Size(max = 200) String provider,
                                     boolean mandatory) {

        public MedicalRecordCommands.RecordVaccination toCommand() {
            return new MedicalRecordCommands.RecordVaccination(vaccine, dose, administeredOn,
                    nextReminderOn, provider, mandatory);
        }
    }

    public record ChronicConditionRequest(@Size(max = 30) String code,
                                          @NotBlank @Size(max = 200) String label,
                                          LocalDate since,
                                          boolean longTermCondition,
                                          @Size(max = 1000) String note) {

        public MedicalRecordCommands.AddChronicCondition toCommand() {
            return new MedicalRecordCommands.AddChronicCondition(code, label, since, longTermCondition,
                    note);
        }
    }

    public record StartTreatmentRequest(@NotBlank @Size(max = 200) String drug,
                                        @Size(max = 100) String dosage,
                                        @Size(max = 100) String frequency,
                                        @Size(max = 200) String prescriber,
                                        LocalDate startedOn,
                                        @Size(max = 1000) String note) {

        public MedicalRecordCommands.StartTreatment toCommand() {
            return new MedicalRecordCommands.StartTreatment(drug, dosage, frequency, prescriber, startedOn,
                    note);
        }
    }

    public record EndTreatmentRequest(@NotNull LocalDate endedOn) {

        public MedicalRecordCommands.EndTreatment toCommand() {
            return new MedicalRecordCommands.EndTreatment(endedOn);
        }
    }

    public record SurgeryRequest(@NotBlank @Size(max = 200) String procedure,
                                 @NotNull LocalDate performedOn,
                                 @Size(max = 200) String establishment,
                                 @Size(max = 200) String surgeon,
                                 @Size(max = 1000) String note) {

        public MedicalRecordCommands.AddSurgery toCommand() {
            return new MedicalRecordCommands.AddSurgery(procedure, performedOn, establishment, surgeon,
                    note);
        }
    }

    public record HospitalizationRequest(@NotBlank @Size(max = 255) String reason,
                                         @NotBlank @Size(max = 200) String establishment,
                                         @NotNull LocalDate admittedOn,
                                         LocalDate dischargedOn,
                                         @Size(max = 1000) String note) {

        public MedicalRecordCommands.AddHospitalization toCommand() {
            return new MedicalRecordCommands.AddHospitalization(reason, establishment, admittedOn,
                    dischargedOn, note);
        }
    }

    public record DisabilityRequest(@NotNull MedicalRecord.DisabilityType type,
                                    @Min(0) @Max(100) Integer disabilityRatePercent,
                                    List<String> aids,
                                    @Size(max = 1000) String note) {

        public MedicalRecordCommands.DeclareDisability toCommand() {
            return new MedicalRecordCommands.DeclareDisability(type, disabilityRatePercent, aids, note);
        }
    }

    public record VitalSignsRequest(Integer systolicMmHg,
                                    Integer diastolicMmHg,
                                    @Min(20) @Max(260) Integer heartRateBpm,
                                    BigDecimal temperatureCelsius,
                                    @Min(0) @Max(100) Integer oxygenSaturationPercent,
                                    @Min(4) @Max(80) Integer respiratoryRatePerMin,
                                    BigDecimal heightCm,
                                    BigDecimal weightKg) {

        public MedicalRecordCommands.RecordVitalSigns toCommand() {
            return new MedicalRecordCommands.RecordVitalSigns(systolicMmHg, diastolicMmHg, heartRateBpm,
                    temperatureCelsius, oxygenSaturationPercent, respiratoryRatePerMin, heightCm, weightKg);
        }
    }
}
