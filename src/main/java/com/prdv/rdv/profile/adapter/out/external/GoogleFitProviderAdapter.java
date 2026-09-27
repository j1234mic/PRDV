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
 * Adapteur Google Fit / Health Connect (Strategy).
 *
 * <p>Point d'extension prevu pour l'API REST Google Fit
 * ({@code users/me/dataSources/.../datasets}) : renseigner les identifiants
 * OAuth du patient, mapper les {@code dataSet} en {@link HealthMetric}. Le
 * contrat du port reste identique, aucun cas d'usage n'est modifie.
 */
@Component
public class GoogleFitProviderAdapter implements ConnectedHealthProviderPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleFitProviderAdapter.class);

    @Override
    public boolean supports(ConnectedDevice.Provider provider) {
        return provider == ConnectedDevice.Provider.GOOGLE_FIT;
    }

    @Override
    public List<HealthMetric> fetch(Long userId, ConnectedDevice device, Instant from, Instant to) {
        log.info("[GOOGLE-FIT] Synchronisation {} de l'appareil {} sur [{} ; {}] :"
                + " acces OAuth non configure, aucune mesure recuperee",
                device.getType(), device.getLabel(), from, to);
        return List.of();
    }

    @Override
    public String name() {
        return "google-fit";
    }
}
