package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.profile.application.port.output.PatientBasicsPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapteur : elements d'etat civil connus du contexte IAM (profil patient
 * cree a l'inscription), utilises pour amorcer l'identite riche du module 2.
 */
@Component
public class IamPatientBasicsAdapter implements PatientBasicsPort {

    private final PatientProfileRepository patientProfiles;

    public IamPatientBasicsAdapter(PatientProfileRepository patientProfiles) {
        this.patientProfiles = patientProfiles;
    }

    @Override
    public Optional<PatientBasics> basicsOf(Long userId) {
        return patientProfiles.findByUserId(userId)
                .map(profile -> new PatientBasics(profile.getFirstName(), profile.getLastName(),
                        profile.getBirthDate(),
                        profile.getGender() == null ? null : profile.getGender().name()));
    }
}
