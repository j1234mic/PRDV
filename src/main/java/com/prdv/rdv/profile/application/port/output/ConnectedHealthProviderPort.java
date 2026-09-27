package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;

import java.time.Instant;
import java.util.List;

/**
 * Port de sortie : fournisseur de donnees de sante connectee (Strategy).
 *
 * <p>Un adapteur par plateforme : Apple Health (HealthKit), Google Fit,
 * Bluetooth SIG (tensiometre, glucometre, balance, oxymetre, ECG portable).
 * Le cas d'usage selectionne l'adapteur selon la plateforme de l'objet
 * connecte : ajouter une plateforme n'implique aucune modification du service.
 */
public interface ConnectedHealthProviderPort {

    boolean supports(ConnectedDevice.Provider provider);

    /** Mesures produites par la plateforme sur la fenetre demandee. */
    List<HealthMetric> fetch(Long userId, ConnectedDevice device, Instant from, Instant to);

    String name();
}
