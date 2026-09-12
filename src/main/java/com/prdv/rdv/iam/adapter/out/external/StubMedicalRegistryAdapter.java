package com.prdv.rdv.iam.adapter.out.external;

import com.prdv.rdv.iam.application.port.output.MedicalRegistryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapteur simule du referentiel RPPS/ADELI et de l'API Ordre des medecins.
 *
 * <p>Regle de simulation : un numero RPPS de 11 chiffres (ou commençant par « TEST »)
 * est considere valide. En production, remplacer par un client HTTP des API
 * officielles (https://annuaire.sante.fr) sans modifier le domaine.
 */
@Component
public class StubMedicalRegistryAdapter implements MedicalRegistryPort {

    private static final Logger log = LoggerFactory.getLogger(StubMedicalRegistryAdapter.class);

    @Override
    public LicenseVerification verify(String rppsNumber, String adeliNumber, String lastName, String firstName) {
        log.info("[MEDICAL-REGISTRY] Verification RPPS={} ADELI={} pour {} {}",
                rppsNumber, adeliNumber, lastName, firstName);
        boolean rppsValid = rppsNumber != null
                && (rppsNumber.matches("\\d{11}") || rppsNumber.startsWith("TEST"));
        boolean adeliValid = adeliNumber == null || adeliNumber.isBlank()
                || adeliNumber.matches("\\d{9}") || adeliNumber.startsWith("TEST");
        // Le diplome est confirme via l'API Ordre des medecins : simule si RPPS valide
        boolean diplomaValid = rppsValid;
        return new LicenseVerification(rppsValid, adeliValid, diplomaValid,
                "stub-annuaire.sante.fr", rppsValid ? "ACTIVE" : "NOT_FOUND");
    }
}
