package com.prdv.rdv.profile.application.service.support;

import com.prdv.rdv.profile.application.port.output.HealthAlertNotifierPort;
import com.prdv.rdv.profile.application.port.output.HealthAlertRepository;
import com.prdv.rdv.profile.application.port.output.HealthMetricRepository;
import com.prdv.rdv.profile.application.port.output.ProfileAuditPort;
import com.prdv.rdv.profile.application.port.output.ProfileEventPublisher;
import com.prdv.rdv.profile.domain.event.ProfileEvent;
import com.prdv.rdv.profile.domain.model.health.HealthAlert;
import com.prdv.rdv.profile.domain.model.health.HealthAlertEvaluator;
import com.prdv.rdv.profile.domain.model.health.HealthMetric;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Enregistrement commun des mesures de sante : persistance, evaluation des
 * alertes automatiques, notification du patient et evenements de domaine.
 *
 * <p>Utilise par la synchronisation des objets connectes, par l'import manuel
 * de mesures et par la saisie des constantes vitales du dossier medical : une
 * valeur saisie a la main suit exactement le meme chemin qu'une mesure d'appareil
 * (graphiques, alertes, historique).
 */
@Component
public class HealthMetricRecorder {

    private final HealthMetricRepository metricRepository;
    private final HealthAlertRepository alertRepository;
    private final HealthAlertEvaluator alertEvaluator;
    private final HealthAlertNotifierPort alertNotifier;
    private final ProfileEventPublisher eventPublisher;
    private final ProfileAuditTrail auditTrail;
    private final Clock clock;

    public HealthMetricRecorder(HealthMetricRepository metricRepository,
                                HealthAlertRepository alertRepository,
                                HealthAlertEvaluator alertEvaluator,
                                HealthAlertNotifierPort alertNotifier,
                                ProfileEventPublisher eventPublisher,
                                ProfileAuditTrail auditTrail,
                                Clock clock) {
        this.metricRepository = metricRepository;
        this.alertRepository = alertRepository;
        this.alertEvaluator = alertEvaluator;
        this.alertNotifier = alertNotifier;
        this.eventPublisher = eventPublisher;
        this.auditTrail = auditTrail;
        this.clock = clock;
    }

    /**
     * Persiste les mesures (la source est renseignee lorsque la mesure n'en
     * porte pas), evalue les alertes et notifie le patient si necessaire.
     */
    public Outcome record(Long userId, String deviceId, List<HealthMetric> metrics, String source) {
        List<HealthMetric> stamped = metrics.stream()
                .map(metric -> metric.deviceId() == null && deviceId != null
                        ? HealthMetric.of(userId, deviceId, metric.type(), metric.value(), metric.context(),
                        metric.recordedAt(), source)
                        : metric)
                .toList();
        List<HealthMetric> saved = metricRepository.saveAll(stamped);
        List<HealthAlert> alerts = alertEvaluator.evaluateAll(saved, userId, clock);
        if (!alerts.isEmpty()) {
            alertRepository.saveAll(alerts);
            alertNotifier.notify(userId, alerts);
            alerts.forEach(alert -> eventPublisher.publish(new ProfileEvent.HealthAlertTriggered(userId,
                    alert.getMetricType().name(), String.valueOf(alert.getObservedValue()),
                    alert.getSeverity().name(), alert.getMessage(), clock.instant())));
            auditTrail.success(ProfileAuditPort.ProfileAuditAction.HEALTH_ALERT_TRIGGERED, userId,
                    "HealthAlert", null, alerts.size() + " alertes declenchees");
        }
        return new Outcome(saved, alerts);
    }

    /** Resultat d'un enregistrement : mesures persistees et alertes declenchees. */
    public record Outcome(List<HealthMetric> accepted, List<HealthAlert> alerts) {
    }
}
