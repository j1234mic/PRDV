package com.prdv.profile.application.port.out;

import java.util.Optional;

/**
 * Port de sortie : "API Ordre des Medecins" (module 1.1).
 * L'adaptateur courant est un STUB deterministe (verification checksum + faux annuaire) ;
 * en production on branche le vrai service sans toucher au coeur (OPEN/CLOSED).
 */
public interface MedicalRegistryGateway {

    record RegistryEntry(String legalName, String specialty, boolean active) { }

    Optional<RegistryEntry> findByRpps(String rppsNumber);
}
