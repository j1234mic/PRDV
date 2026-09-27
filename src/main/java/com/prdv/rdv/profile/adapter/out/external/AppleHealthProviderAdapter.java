package com.prdv.rdv.profile.adapter.out.external;

import com.prdv.rdv.profile.application.port.output.ConnectedHealthProviderPort;
import com.prdv.rdv.profile.domain.model.health.ConnectedDevice;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Adapteur Apple Health / HealthKit (Strategy).
 *
 * <p>HealthKit ne s'interroge pas depuis un serveur : les mesures sont
 * poussees par l'application mobile (endpoint d'import par lot). Cet
 * adapteur borne donc la fenetre demandee et renvoie une liste vide, ce qui
 * rend le contrat explicite ; l'import reel passe par
 * {@code ConnectedHealthUseCase.ingest}.
 */
@Component
public class AppleHealthProviderAdapter implements ConnectedHealthProviderPort {

    private static final Logger log = LoggerFactory.getLogger(AppleHealthProviderAdapter.class);

    @Override
    public boolean supports(ConnectedDevice.Provider provider) {
        return provider == ConnectedDevice.Provider.APPLE_HEALTH;
    }

    @Override
    public List<HealthMetric> fetch(Long userId, ConnectedDevice device, Instant from, Instant to) {
        log.info("[APPLE-HEALTH] Synchronisation {} de l'appareil {} sur [{} ; {}] :"
                        + " les donnees HealthKit sont poussees par l'application mobile",
                device.getType(), device.getLabel(), from, to);
        return List.of();
    }

    @Override
    public String name() {
        return "apple-healthkit";
    }
}
