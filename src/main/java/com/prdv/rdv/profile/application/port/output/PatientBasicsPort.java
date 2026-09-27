package com.prdv.rdv.profile.application.port.output;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Port de sortie : elements d'etat civil deja connus du contexte IAM
 * (profil patient cree a l'inscription). Sert a amorcer l'identite riche du
 * module profils sans que ce contexte ne lise les tables de l'autre.
 */
public interface PatientBasicsPort {

    Optional<PatientBasics> basicsOf(Long userId);

    record PatientBasics(String firstName, String lastName, LocalDate birthDate, String gender) {
    }
}
