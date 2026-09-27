package com.prdv.rdv.profile.adapter.out.notification;

import com.prdv.rdv.profile.application.port.output.HealthAlertNotifierPort;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Notification des alertes de sante en developpement (journalisation).
 *
 * <p>En production, un adapteur envoie une notification push / SMS selon la
 * gravite (CRITICAL -&gt; SMS immediat, WARNING -&gt; push) et respecte les
 * preferences de communication et les horaires de tranquillite du patient.
 */
@Component
public class LoggingHealthAlertNotifierAdapter implements HealthAlertNotifierPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingHealthAlertNotifierAdapter.class);

    @Override
    public void notify(Long userId, List<HealthAlert> alerts) {
        for (HealthAlert alert : alerts) {
            log.warn("[ALERTE SANTE] utilisateur {} - {} = {} (seuil {} {}) : {}", userId,
                    alert.getMetricType(), alert.getObservedValue(), alert.getThreshold(),
                    alert.getBoundary(), alert.getMessage());
        }
    }
}
