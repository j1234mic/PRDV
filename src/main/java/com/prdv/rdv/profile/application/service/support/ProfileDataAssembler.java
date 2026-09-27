package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.ConnectedDeviceRepository;
import com.prdv.rdv.profile.application.port.output.MedicalDocumentRepository;
import com.prdv.rdv.profile.application.port.output.MedicalRecordRepository;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.PracticeLocationRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerDossierRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerRatingRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

    private final PatientIdentityRepository identityRepository;
    private final MedicalRecordRepository recordRepository;
    private final MedicalDocumentRepository documentRepository;
    private final ConnectedDeviceRepository deviceRepository;
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
        data.put("privacyAndConsents",
                viewMapper.preferencesView(accessGuard.preferencesOf(userId)));

        LocalDateTime now = LocalDateTime.now(clock);
        data.put("practiceLocations", locationRepository.findByPractitionerUserId(userId).stream()
                .map(location -> viewMapper.locationView(location, now)).toList());
        data.put("ratingsGivenAndReceived", ratingRepository.findByPractitionerUserId(userId).stream()
                .map(viewMapper::ratingView).toList());
        dossierRepository.findByUserId(userId).ifPresent(dossier -> data.put("professionalProfile",
                viewMapper.dossierView(dossier, null, java.util.List.of(), java.util.List.of())));
        return data;
    }
}
