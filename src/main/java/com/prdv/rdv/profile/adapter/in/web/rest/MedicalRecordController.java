package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.profile.adapter.in.web.dto.MedicalRecordDtos;
import com.prdv.rdv.profile.application.port.input.MedicalRecordCommandUseCase;
import com.prdv.rdv.profile.application.port.input.MedicalRecordQueryUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dossier medical personnel (module 2.1) : antecedents structures et familiaux,
 * allergies, vaccinations avec rappels, maladies chroniques, traitements,
 * chirurgies, hospitalisations, handicaps, groupe sanguin et constantes
 * vitales (IMC calcule par le domaine).
 *
 * <p>La lecture par un tiers passe par {@code recordOf} : le service verifie la
 * regle de visibilite et le consentement avant de renvoyer quoi que ce soit.
 */
@RestController
@RequestMapping("/api/v1/medical-record")
@Tag(name = "Dossier medical", description = "Antecedents, allergies, vaccins, traitements, constantes")
public class MedicalRecordController {

    private static final int DEFAULT_REMINDER_LOOKAHEAD_DAYS = 60;

    private final MedicalRecordQueryUseCase queryUseCase;
    private final MedicalRecordCommandUseCase commandUseCase;

    public MedicalRecordController(MedicalRecordQueryUseCase queryUseCase,
                                   MedicalRecordCommandUseCase commandUseCase) {
        this.queryUseCase = queryUseCase;
        this.commandUseCase = commandUseCase;
    }

    // ------------------------------------------------------------------
    // Lectures
    // ------------------------------------------------------------------

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('medicalrecord.read')")
    public ProfileViews.MedicalRecordView myRecord() {
        return queryUseCase.myRecord();
    }

    @GetMapping("/{patientUserId}")
    @PreAuthorize("hasAuthority('medicalrecord.read')")
    @Operation(summary = "Dossier d'un patient par un tiers (controle de visibilite)")
    public ProfileViews.MedicalRecordView recordOf(@PathVariable Long patientUserId) {
        return queryUseCase.recordOf(patientUserId);
    }

    @GetMapping("/{patientUserId}/vaccination-reminders")
    @PreAuthorize("hasAuthority('medicalrecord.read')")
    public List<ProfileViews.VaccinationReminderView> vaccinationReminders(
            @PathVariable Long patientUserId,
            @RequestParam(defaultValue = "" + DEFAULT_REMINDER_LOOKAHEAD_DAYS) int lookaheadDays) {
        return queryUseCase.vaccinationReminders(patientUserId, lookaheadDays);
    }

    // ------------------------------------------------------------------
    // Ecritures
    // ------------------------------------------------------------------

    @PutMapping("/{patientUserId}/blood-group")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView setBloodGroup(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.BloodGroupRequest request) {
        return commandUseCase.setBloodGroup(patientUserId, request.toCommand());
    }

    @PostMapping("/{patientUserId}/history")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView addHistoryEntry(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.HistoryEntryRequest request) {
        return commandUseCase.addHistoryEntry(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/history/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHistoryEntry(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeHistoryEntry(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/family-history")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView addFamilyHistory(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.FamilyHistoryRequest request) {
        return commandUseCase.addFamilyHistory(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/family-history/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFamilyHistory(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeFamilyHistory(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/allergies")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView declareAllergy(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.AllergyRequest request) {
        return commandUseCase.declareAllergy(patientUserId, request.toCommand());
    }

    @PostMapping("/{patientUserId}/allergies/{entryId}/resolve")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @Operation(summary = "Marque une allergie comme resolue sans la supprimer")
    public ProfileViews.MedicalRecordView resolveAllergy(@PathVariable Long patientUserId,
                                                         @PathVariable String entryId) {
        return commandUseCase.resolveAllergy(patientUserId, entryId);
    }

    @DeleteMapping("/{patientUserId}/allergies/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAllergy(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeAllergy(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/vaccinations")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView recordVaccination(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.VaccinationRequest request) {
        return commandUseCase.recordVaccination(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/vaccinations/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVaccination(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeVaccination(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/chronic-conditions")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView addChronicCondition(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.ChronicConditionRequest request) {
        return commandUseCase.addChronicCondition(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/chronic-conditions/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeChronicCondition(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeChronicCondition(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/treatments")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView startTreatment(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.StartTreatmentRequest request) {
        return commandUseCase.startTreatment(patientUserId, request.toCommand());
    }

    @PostMapping("/{patientUserId}/treatments/{entryId}/end")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView endTreatment(
            @PathVariable Long patientUserId,
            @PathVariable String entryId,
            @Valid @RequestBody MedicalRecordDtos.EndTreatmentRequest request) {
        return commandUseCase.endTreatment(patientUserId, entryId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/treatments/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTreatment(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeTreatment(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/surgeries")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView addSurgery(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.SurgeryRequest request) {
        return commandUseCase.addSurgery(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/surgeries/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSurgery(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeSurgery(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/hospitalizations")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView addHospitalization(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.HospitalizationRequest request) {
        return commandUseCase.addHospitalization(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/hospitalizations/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeHospitalization(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeHospitalization(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/disabilities")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    public ProfileViews.MedicalRecordView declareDisability(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.DisabilityRequest request) {
        return commandUseCase.declareDisability(patientUserId, request.toCommand());
    }

    @DeleteMapping("/{patientUserId}/disabilities/{entryId}")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDisability(@PathVariable Long patientUserId, @PathVariable String entryId) {
        commandUseCase.removeDisability(patientUserId, entryId);
    }

    @PostMapping("/{patientUserId}/vital-signs")
    @PreAuthorize("hasAuthority('medicalrecord.write')")
    @Operation(summary = "Constantes vitales : l'IMC est calcule par le domaine")
    public ProfileViews.MedicalRecordView recordVitalSigns(
            @PathVariable Long patientUserId,
            @Valid @RequestBody MedicalRecordDtos.VitalSignsRequest request) {
        return commandUseCase.recordVitalSigns(patientUserId, request.toCommand());
    }
}
