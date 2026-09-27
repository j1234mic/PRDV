package com.prdv.rdv.profile.adapter.out.event;

import com.prdv.rdv.profile.domain.event.ProfileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Abonne de demonstration aux evenements du contexte profils.
 *
 * <p>Illustre l'extensibilite du mecanisme (Observer) : notifications
 * patients, alimentation d'un entrepot analytics, invalidation de cache ou
 * transmission au SIEM s'ajoutent par de nouveaux abonnes, sans jamais
 * modifier les cas d'usage.
 */
@Component
public class ProfileEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileEventListener.class);

    @EventListener
    public void onDocumentShared(ProfileEvent.MedicalDocumentShared event) {
        log.info("[EVENEMENT] Document {} partage par {} avec {} (droit {})", event.documentId(),
                event.userId(), event.granteeUserId(), event.permission());
    }

    @EventListener
    public void onHealthAlert(ProfileEvent.HealthAlertTriggered event) {
        log.warn("[EVENEMENT] Alerte {} pour {} : {} = {} ({})", event.severity(), event.userId(),
                event.metricType(), event.value(), event.message());
    }

    @EventListener
    public void onConsent(ProfileEvent.ConsentRecorded event) {
        log.info("[EVENEMENT] Consentement {} {} par {}", event.purpose(),
                event.granted() ? "accorde" : "retire", event.userId());
    }

    @EventListener
    public void onErasure(ProfileEvent.ProfileErasureRequested event) {
        log.warn("[EVENEMENT] Demande d'effacement des donnees de profil de l'utilisateur {}",
                event.userId());
    }

    @EventListener
    public void onBadgeGranted(ProfileEvent.PractitionerBadgeGranted event) {
        log.info("[EVENEMENT] Badge {} attribue au praticien {} ({})", event.badge(), event.userId(),
                event.reason());
    }
}
