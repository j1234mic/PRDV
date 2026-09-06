package com.prdv.adapters.out.registry;

import com.prdv.profile.application.port.out.MedicalRegistryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * STUB de l'API "Ordre des Medecins" / annuaire RPPS (module 1.1).
 * En dev/demo : tout numero RPPS structurellement valide est consideré inscrit.
 * En prod : remplacer ce bean par un appel HTTP (annuaire.sante.fr / API ASIP)
 * sans toucher au coeur : c'est l'interet exact du port MedicalRegistryGateway.
 */
@Component
public class StubMedicalRegistryGateway implements MedicalRegistryGateway {

    private static final Logger log = LoggerFactory.getLogger(StubMedicalRegistryGateway.class);

    @Override
    public Optional<RegistryEntry> findByRpps(String rppsNumber) {
        log.info("[STUB Ordre des Medecins] verification RPPS {} - reponse simulee ACTIVE", rppsNumber);
        if (rppsNumber == null || !rppsNumber.matches("\\d{11}")) {
            return Optional.empty();
        }
        return Optional.of(new RegistryEntry("", null, true));
    }
}
