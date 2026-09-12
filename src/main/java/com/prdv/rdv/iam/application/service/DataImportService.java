package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.port.input.DataImportUseCase;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.ProfileImportPort;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Import des donnees patient depuis une autre plateforme.
 * Le format (CSV, JSON, API Doctolib/Maiia...) est choisi via la Strategie
 * {@link ProfileImportPort} : ajouter un fournisseur ne modifie pas ce service
 * (principe Open/Closed - couche anti-corruption).
 */
@Service
public class DataImportService implements DataImportUseCase {

    private final List<ProfileImportPort> importers;
    private final PatientProfileRepository patientProfileRepository;
    private final SecurityContextPort securityContext;
    private final Clock clock;

    public DataImportService(List<ProfileImportPort> importers,
                             PatientProfileRepository patientProfileRepository,
                             SecurityContextPort securityContext, Clock clock) {
        this.importers = importers;
        this.patientProfileRepository = patientProfileRepository;
        this.securityContext = securityContext;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Views.ImportSummary importMyProfile(ProfileCommands.ImportExternalProfile command) {
        ProfileImportPort importer = importers.stream()
                .filter(port -> port.supports(command.format()))
                .findFirst()
                .orElseThrow(() -> IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID,
                        "Format d'import non pris en charge : " + command.format()));

        ProfileImportPort.ImportedProfile data;
        try {
            data = importer.parse(command.rawContent());
        } catch (IamException e) {
            throw e;
        } catch (RuntimeException e) {
            throw IamException.of(IamErrorCode.IMPORT_FORMAT_INVALID,
                    "Contenu " + command.format() + " invalide : " + e.getMessage());
        }

        Long userId = securityContext.requireCurrentUserId();
        PatientProfile profile = patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> IamException.of(IamErrorCode.PROFILE_MISMATCH, "Profil patient introuvable"));

        List<String> updated = new ArrayList<>();
        if (isBlank(profile.getFirstName()) && data.firstName() != null) {
            profile.setFirstName(data.firstName());
            updated.add("firstName");
        }
        if (isBlank(profile.getLastName()) && data.lastName() != null) {
            profile.setLastName(data.lastName());
            updated.add("lastName");
        }
        if (profile.getBirthDate() == null && data.birthDate() != null) {
            profile.setBirthDate(data.birthDate());
            updated.add("birthDate");
        }
        profile.setImportedFrom(data.sourcePlatform());
        profile.setUpdatedAt(clock.instant());
        patientProfileRepository.save(profile);

        return new Views.ImportSummary(data.sourcePlatform(), updated.size(), updated);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
