package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.ConnectedDeviceRepository;
import com.prdv.rdv.profile.application.port.output.HealthAlertRepository;
import com.prdv.rdv.profile.application.port.output.HealthMetricRepository;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assemble l'arborescence de donnees d'un utilisateur en vue de l'export de
 * portabilite (RGPD art. 20).
 *
 * <p>Isoler cet assemblage (Single Responsibility) permet d'ajouter des
 * sections sans toucher au cas d'usage, et de produire le meme arbre quel que
 * soit le format de sortie (JSON, CSV, FHIR...) choisi par
 * {@code DataExportPort}.
 */
@Component
public class ProfileDataAssembler {

    /** Borne haute de la fenetre d'export des mesures (toutes les mesures existantes). */
    private static final Instant EXPORT_FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");
    /** Plafond de mesures exportees : au-dela, l'export reste borne et le volume complet passe par l'API. */
    static final int MAX_EXPORTED_METRICS = 50_000;

    private final PatientIdentityRepository identityRepository;
    private final MedicalRecordRepository recordRepository;
    private final MedicalDocumentRepository documentRepository;
    private final ConnectedDeviceRepository deviceRepository;
    private final HealthMetricRepository metricRepository;
    private final HealthAlertRepository alertRepository;
    private final PrivacyPreferencesRepository preferencesRepository;
    private final PractitionerDossierRepository dossierRepository;
    private final PracticeLocationRepository locationRepository;
    private final PractitionerRatingRepository ratingRepository;
    private final ProfileAccessGuard accessGuard;
    private final ProfileViewMapper viewMapper;
    private final Clock clock;

    public ProfileDataAssembler(PatientIdentityRepository identityRepository,
                                MedicalRecordRepository recordRepository,
                                MedicalDocumentRepository documentRepository,
                                ConnectedDeviceRepository deviceRepository,
                                HealthMetricRepository metricRepository,
                                HealthAlertRepository alertRepository,
                                PrivacyPreferencesRepository preferencesRepository,
                                PractitionerDossierRepository dossierRepository,
                                PracticeLocationRepository locationRepository,
                                PractitionerRatingRepository ratingRepository,
                                ProfileAccessGuard accessGuard,
                                ProfileViewMapper viewMapper,
                                Clock clock) {
        this.identityRepository = identityRepository;
        this.recordRepository = recordRepository;
        this.documentRepository = documentRepository;
        this.deviceRepository = deviceRepository;
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.preferencesRepository = preferencesRepository;
        this.dossierRepository = dossierRepository;
        this.locationRepository = locationRepository;
        this.ratingRepository = ratingRepository;
        this.accessGuard = accessGuard;
        this.viewMapper = viewMapper;
        this.clock = clock;
    }

    public Map<String, Object> assemble(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", clock.instant().toString());
        data.put("subject", Map.of("userId", userId));

        identityRepository.findByUserId(userId)
                .ifPresent(identity -> data.put("personalInformation", viewMapper.identityView(identity)));
        recordRepository.findByPatientUserId(userId)
                .ifPresent(record -> data.put("medicalRecord", viewMapper.medicalRecordView(record)));

        data.put("medicalDocuments", documentRepository.findByOwnerUserId(userId).stream()
                .map(viewMapper::documentView).toList());
        data.put("connectedDevices", deviceRepository.findByUserId(userId).stream()
                .map(viewMapper::deviceView).toList());
        data.put("healthMetrics", metricRepository
                .find(userId, null, Instant.EPOCH, EXPORT_FAR_FUTURE, MAX_EXPORTED_METRICS).stream()
                .map(viewMapper::metricView).toList());
        data.put("healthAlerts", alertRepository.findByUserId(userId, false).stream()
                .map(viewMapper::alertView).toList());
        // Lecture sans effet de bord : l'export ne doit jamais inserer de preferences.
        data.put("privacyAndConsents",
                viewMapper.preferencesView(accessGuard.preferencesOrDefaults(userId)));

        LocalDateTime now = LocalDateTime.now(clock);
        data.put("practiceLocations", locationRepository.findByPractitionerUserId(userId).stream()
                .map(location -> viewMapper.locationView(location, now)).toList());
        data.put("ratingsGivenAndReceived", ratingRepository.findByPractitionerUserId(userId).stream()
                .map(viewMapper::ratingView).toList());
        dossierRepository.findByUserId(userId).ifPresent(dossier -> data.put("professionalProfile",
                viewMapper.dossierView(dossier, null, List.of(), List.of())));
        return data;
    }
}
