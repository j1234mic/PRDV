package com.prdv.rdv.iam.application.port.output;

import java.time.LocalDate;

/**
 * Port « anti-corruption » d'import des donnees depuis une autre plateforme
 * (Doctolib, Maiia, export CSV...). Chaque adapteur traduit le format source
 * en un modele canonique independant du format.
 */
public interface ProfileImportPort {

    boolean supports(String format);

    ImportedProfile parse(String rawContent);

    record ImportedProfile(String firstName, String lastName, LocalDate birthDate,
                           String email, String phone, String sourcePlatform) {
    }
}
