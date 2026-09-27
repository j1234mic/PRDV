package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.port.input.MedicalRecordQueryUseCase;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAccessGuard;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileViewMapper;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Cas d'usage : lecture du dossier medical personnel.
 *
 * <p>Chaque acces croise (un praticien consulte le dossier d'un patient) est
 * journalise : c'est une exigence du secret medical et de la tracabilite
 * RGPD, pas un simple log technique.
 */
@Service
public class MedicalRecordQueryService implements MedicalRecordQueryUseCase {

    private final MedicalRecordRepository recordRepository;
    private final CurrentUserPort currentUser;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final ProfileAuditTrail auditTrail;
    private final Clock clock;

    public MedicalRecordQueryService(MedicalRecordRepository recordRepository,
                                     CurrentUserPort currentUser,
                                     ProfileAccessGuard accessGuard,
                                     ProfileViewMapper viewMapper,
                                     ProfileAuditTrail auditTrail,
                                     Clock clock) {
        this.recordRepository = recordRepository;
        this.currentUser = currentUser;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.MedicalRecordView myRecord() {
        Long userId = currentUser.requireCurrentUserId();
        return viewMapper.medicalRecordView(loadOrEmpty(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.MedicalRecordView recordOf(Long patientUserId) {
        accessGuard.requireAccess(patientUserId, PrivacyPreferences.DataCategory.MEDICAL_RECORD);
        MedicalRecord record = loadOrEmpty(patientUserId);
        Long actor = currentUser.requireCurrentUserId();
        if (!actor.equals(patientUserId)) {
            auditTrail.success(ProfileAuditPort.ProfileAuditAction.MEDICAL_RECORD_ACCESSED, actor,
                    "MedicalRecord", String.valueOf(patientUserId),
                    "consultation du dossier d'un patient");
        }
        return viewMapper.medicalRecordView(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileViews.VaccinationReminderView> vaccinationReminders(Long patientUserId,
                                                                           int lookaheadDays) {
        accessGuard.requireAccess(patientUserId, PrivacyPreferences.DataCategory.MEDICAL_RECORD);
        return viewMapper.reminderViews(loadOrEmpty(patientUserId), lookaheadDays);
    }

    private MedicalRecord loadOrEmpty(Long patientUserId) {
        return recordRepository.findByPatientUserId(patientUserId)
                .orElseGet(() -> MedicalRecord.open(patientUserId, clock));
    }
}
