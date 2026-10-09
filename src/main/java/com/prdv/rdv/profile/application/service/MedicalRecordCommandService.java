package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.MedicalRecordCommands;
import com.prdv.rdv.profile.application.port.input.MedicalRecordCommandUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.HealthMetricRecorder;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import com.prdv.rdv.profile.domain.model.medical.BloodGroup;
import com.prdv.rdv.profile.domain.model.medical.BodyMetrics;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.medical.VitalSigns;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cas d'usage : ecriture dans le dossier medical personnel (module 2.1 — DMP).
 *
 * <p>Toutes les mutations suivent le meme canevas (chargement de l'agregat,
 * application de la regle metier portee par le domaine, persistance, audit,
 * evenement) : le service reste une orchestration lisible et chaque section
 * du dossier est tracee individuellement.
 */
@Service
public class MedicalRecordCommandService implements MedicalRecordCommandUseCase {

    private final MedicalRecordRepository recordRepository;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final HealthMetricRecorder healthMetricRecorder;
    private final Clock clock;

    public MedicalRecordCommandService(MedicalRecordRepository recordRepository,
                                       CurrentUserPort currentUser,
                                       ProfileAccessGuard accessGuard,
                                       ProfileViewMapper viewMapper,
                                       ProfileAuditTrail auditTrail,
                                       ProfileEventPublisher eventPublisher,
                                       HealthMetricRecorder healthMetricRecorder,
                                       Clock clock) {
        this.healthMetricRecorder = healthMetricRecorder;
        this.recordRepository = recordRepository;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView setBloodGroup(Long patientUserId,
                                                        MedicalRecordCommands.SetBloodGroup command) {
        BloodGroup bloodGroup = BloodGroup.of(command.value());
        return apply(patientUserId, "groupe sanguin",
                record -> record.setBloodGroupValue(bloodGroup, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView addHistoryEntry(Long patientUserId,
                                                          MedicalRecordCommands.AddHistoryEntry command) {
        return apply(patientUserId, "antecedent medical", record -> record.addHistoryEntry(command.code(),
                command.label(), command.diagnosedOn(), command.status(), command.note(),
                command.source(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeHistoryEntry(Long patientUserId, String entryId) {
        return apply(patientUserId, "antecedent medical",
                record -> record.removeHistoryEntry(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView addFamilyHistory(Long patientUserId,
                                                           MedicalRecordCommands.AddFamilyHistory command) {
        return apply(patientUserId, "antecedent familial", record -> record.addFamilyHistoryEntry(
                command.relation(), command.condition(), command.relativeAgeAtDiagnosis(),
                command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeFamilyHistory(Long patientUserId, String entryId) {
        return apply(patientUserId, "antecedent familial",
                record -> record.removeFamilyHistoryEntry(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView declareAllergy(Long patientUserId,
                                                         MedicalRecordCommands.DeclareAllergy command) {
        return apply(patientUserId, "allergie", record -> record.declareAllergy(command.allergen(),
                command.type(), command.severity(), command.reaction(), command.declaredOn(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView resolveAllergy(Long patientUserId, String entryId) {
        return apply(patientUserId, "allergie", record -> record.resolveAllergy(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeAllergy(Long patientUserId, String entryId) {
        return apply(patientUserId, "allergie", record -> record.removeAllergy(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView recordVaccination(Long patientUserId,
                                                            MedicalRecordCommands.RecordVaccination command) {
        return apply(patientUserId, "vaccination", record -> record.recordVaccination(command.vaccine(),
                command.dose(), command.administeredOn(), command.nextReminderOn(), command.provider(),
                command.mandatory(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeVaccination(Long patientUserId, String entryId) {
        return apply(patientUserId, "vaccination", record -> record.removeVaccination(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView addChronicCondition(
            Long patientUserId, MedicalRecordCommands.AddChronicCondition command) {
        return apply(patientUserId, "maladie chronique", record -> record.addChronicCondition(
                command.code(), command.label(), command.since(), command.longTermCondition(),
                command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeChronicCondition(Long patientUserId, String entryId) {
        return apply(patientUserId, "maladie chronique",
                record -> record.removeChronicCondition(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView startTreatment(Long patientUserId,
                                                         MedicalRecordCommands.StartTreatment command) {
        return apply(patientUserId, "traitement", record -> record.startTreatment(command.drug(),
                command.dosage(), command.frequency(), command.prescriber(), command.startedOn(),
                command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView endTreatment(Long patientUserId, String entryId,
                                                       MedicalRecordCommands.EndTreatment command) {
        return apply(patientUserId, "traitement",
                record -> record.endTreatment(entryId, command.endedOn(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeTreatment(Long patientUserId, String entryId) {
        return apply(patientUserId, "traitement", record -> record.removeTreatment(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView addSurgery(Long patientUserId,
                                                     MedicalRecordCommands.AddSurgery command) {
        return apply(patientUserId, "chirurgie", record -> record.addSurgery(command.procedure(),
                command.performedOn(), command.establishment(), command.surgeon(), command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeSurgery(Long patientUserId, String entryId) {
        return apply(patientUserId, "chirurgie", record -> record.removeSurgery(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView addHospitalization(
            Long patientUserId, MedicalRecordCommands.AddHospitalization command) {
        return apply(patientUserId, "hospitalisation", record -> record.addHospitalization(
                command.reason(), command.establishment(), command.admittedOn(), command.dischargedOn(),
                command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeHospitalization(Long patientUserId, String entryId) {
        return apply(patientUserId, "hospitalisation",
                record -> record.removeHospitalization(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView declareDisability(Long patientUserId,
                                                            MedicalRecordCommands.DeclareDisability command) {
        return apply(patientUserId, "handicap", record -> record.declareDisability(command.type(),
                command.disabilityRatePercent(), command.aids(), command.note(), clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView removeDisability(Long patientUserId, String entryId) {
        return apply(patientUserId, "handicap", record -> record.removeDisability(entryId, clock));
    }

    @Override
    @Transactional
    public ProfileViews.MedicalRecordView recordVitalSigns(Long patientUserId,
                                                           MedicalRecordCommands.RecordVitalSigns command) {
        BodyMetrics bodyMetrics = command.heightCm() != null && command.weightKg() != null
                ? new BodyMetrics(command.heightCm(), command.weightKg())
                : null;
        VitalSigns signs = new VitalSigns(command.systolicMmHg(), command.diastolicMmHg(),
                command.heartRateBpm(), command.temperatureCelsius(), command.oxygenSaturationPercent(),
                command.respiratoryRatePerMin(), bodyMetrics, clock.instant());
        ProfileViews.MedicalRecordView view = apply(patientUserId, "constantes vitales",
                record -> record.recordVitalSigns(signs, clock));
        // Une saisie manuelle suit le chemin des objets connectes : graphiques, alertes automatiques, historique.
        List<HealthMetric> metrics = signs.toHealthMetrics(patientUserId, "MANUAL");
        if (!metrics.isEmpty()) {
            healthMetricRecorder.record(patientUserId, null, metrics, "MANUAL");
        }
        return view;
    }

    // ------------------------------------------------------------------

    /**
     * Canevas unique d'ecriture : controle d'acces, regle metier dans
     * l'agregat, persistance, audit, evenement de domaine.
     */
    private ProfileViews.MedicalRecordView apply(Long patientUserId, String section,
                                                 Consumer<MedicalRecord> mutation) {
        accessGuard.requireAccess(patientUserId, PrivacyPreferences.DataCategory.MEDICAL_RECORD);
        MedicalRecord record = recordRepository.findByPatientUserId(patientUserId)
                .orElseGet(() -> MedicalRecord.open(patientUserId, clock));
        mutation.accept(record);
        MedicalRecord saved = recordRepository.save(record);

        Long actor = currentUser.requireCurrentUserId();
        auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_RECORD_UPDATED, actor,
                "MedicalRecord", String.valueOf(saved.getId()), section);
        eventPublisher.publish(new ProfileEvent.MedicalRecordUpdated(patientUserId, section,
                clock.instant()));
        return viewMapper.medicalRecordView(saved);
    }
}
