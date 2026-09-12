package com.prdv.rdv.iam.application.port.output;

/**
 * Port anti-corruption (ACL) vers les referentiels medicaux officiels :
 * verification RPPS / ADELI automatique et diplomes via l'API de
 * l'Ordre des medecins. Un adapteur simule est fourni ; en production,
 * brancher les API reelles sans impacter le domaine.
 */
public interface MedicalRegistryPort {

    LicenseVerification verify(String rppsNumber, String adeliNumber, String lastName, String firstName);

    record LicenseVerification(boolean rppsValid, boolean adeliValid, boolean diplomaValid,
                               String provider, String rawStatus) {
    }
}
