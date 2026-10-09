package com.prdv.rdv.profile.application.service;

import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.application.port.input.DataPortabilityUseCase;
import com.prdv.rdv.profile.application.port.output.AccountErasurePort;
import com.prdv.rdv.profile.application.port.output.ConnectedDeviceRepository;
import com.prdv.rdv.profile.application.port.output.CurrentUserPort;
import com.prdv.rdv.profile.application.port.output.DataExportPort;
import com.prdv.rdv.profile.application.port.output.HealthAlertRepository;
import com.prdv.rdv.profile.application.port.output.HealthMetricRepository;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.application.service.support.ProfileAuditTrail;
import com.prdv.rdv.profile.application.service.support.ProfileDataAssembler;
import com.prdv.rdv.profile.application.service.support.ReplacedFileCleaner;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Cas d'usage : portabilite et effacement des donnees (RGPD art. 20 et 17).
 *
 * <p>L'export est genere dans un format lisible par machine ; l'effacement
 * purge les fichiers stockes (identite, carte Vitale, documents, photos et
 * videos professionnelles, photos de cabinet), les mesures de sante, les
 * documents et les consentements, puis delegue l'anonymisation du compte au
 * contexte IAM.
 */
@Service
public class DataPortabilityService implements DataPortabilityUseCase {

    private final ProfileDataAssembler dataAssembler;
    private final DataExportPort dataExport;
    private final AccountErasurePort accountErasure;
    private final PatientIdentityRepository identityRepository;
    private final MedicalRecordRepository recordRepository;
    private final MedicalDocumentRepository documentRepository;
    private final ReplacedFileCleaner replacedFileCleaner;
    private final PrivacyPreferencesRepository preferencesRepository;
    private final ConnectedDeviceRepository deviceRepository;
    private final HealthMetricRepository metricRepository;
    private final HealthAlertRepository alertRepository;
    private final PractitionerDossierRepository dossierRepository;
    private final PracticeLocationRepository locationRepository;
    private final PractitionerRatingRepository ratingRepository;
    private final CurrentUserPort currentUser;
    private final ProfileAuditTrail auditTrail;
    private final ProfileEventPublisher eventPublisher;
    private final Clock clock;

    public DataPortabilityService(ProfileDataAssembler dataAssembler,
                                  DataExportPort dataExport,
                                  AccountErasurePort accountErasure,
                                  PatientIdentityRepository identityRepository,
                                  MedicalRecordRepository recordRepository,
                                  MedicalDocumentRepository documentRepository,
                                  ReplacedFileCleaner replacedFileCleaner,
                                  PrivacyPreferencesRepository preferencesRepository,
                                  ConnectedDeviceRepository deviceRepository,
                                  HealthMetricRepository metricRepository,
                                  HealthAlertRepository alertRepository,
                                  PractitionerDossierRepository dossierRepository,
                                  PracticeLocationRepository locationRepository,
                                  PractitionerRatingRepository ratingRepository,
                                  CurrentUserPort currentUser,
                                  ProfileAuditTrail auditTrail,
                                  ProfileEventPublisher eventPublisher,
                                  Clock clock) {
        this.dataAssembler = dataAssembler;
        this.dataExport = dataExport;
        this.accountErasure = accountErasure;
        this.identityRepository = identityRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.replacedFileCleaner = replacedFileCleaner;
        this.preferencesRepository = preferencesRepository;
        this.deviceRepository = deviceRepository;
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.dossierRepository = dossierRepository;
        this.locationRepository = locationRepository;
        this.ratingRepository = ratingRepository;
        this.currentUser = currentUser;
        this.auditTrail = auditTrail;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileViews.DataExportView exportMyData(String format) {
        Long userId = currentUser.requireCurrentUserId();
        Map<String, Object> data = dataAssembler.assemble(userId);
        DataExportPort.ExportedData exported = dataExport.export(format, data);

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.DATA_EXPORTED, userId, "DataExport", null,
                exported.filename() + " (" + exported.content().length() + " caracteres)");
        eventPublisher.publish(new ProfileEvent.PatientDataExported(userId, format == null
                ? "json" : format, clock.instant()));
        return new ProfileViews.DataExportView(exported.filename(), exported.contentType(),
                exported.content().length(), exported.content());
    }

    @Override
    @Transactional
    public void requestErasure(PrivacyCommands.RequestErasure command) {
        Long userId = currentUser.requireCurrentUserId();

        // Les cles de fichiers sont recensees AVANT l'effacement des fiches (qui les reinitialise),
        // puis les fichiers sont supprimes une fois la transaction validee.
        List<String> storedFiles = new ArrayList<>();

        identityRepository.findByUserId(userId).ifPresent(identity -> {
            storedFiles.addAll(identity.mediaStorageKeys());
            identity.erase(clock);
            identityRepository.save(identity);
        });
        recordRepository.findByPatientUserId(userId).ifPresent(record -> {
            record.erase(clock);
            recordRepository.save(record);
        });

        documentRepository.findByOwnerUserId(userId).forEach(document -> document.getVersions()
                .forEach(version -> storedFiles.add(version.storageKey())));
        documentRepository.deleteByOwnerUserId(userId);

        deviceRepository.deleteByUserId(userId);
        metricRepository.deleteByUserId(userId);
        alertRepository.deleteByUserId(userId);

        preferencesRepository.findByUserId(userId).ifPresent(preferences -> {
            preferences.revokeEverything(command.ipAddress(), clock);
            preferencesRepository.save(preferences);
        });
        preferencesRepository.deleteByUserId(userId);

        dossierRepository.findByUserId(userId).ifPresent(dossier -> {
            storedFiles.addAll(dossier.mediaStorageKeys());
            dossier.erase(clock);
            dossierRepository.save(dossier);
        });
        locationRepository.findByPractitionerUserId(userId)
                .forEach(location -> storedFiles.addAll(location.mediaStorageKeys()));
        locationRepository.deleteByPractitionerUserId(userId);
        ratingRepository.deleteByPatientUserId(userId);

        replacedFileCleaner.discardAfterCommit(storedFiles);

        accountErasure.requestAccountAnonymization(userId);

        auditTrail.success(ProfileAuditPort.ProfileAuditAction.DATA_ERASURE_REQUESTED, userId,
                "ProfileErasure", String.valueOf(userId), "donnees de profil effacees ("
                        + storedFiles.size() + " fichiers a supprimer)");
        eventPublisher.publish(new ProfileEvent.ProfileErasureRequested(userId, clock.instant()));
    }
}
