package com.prdv.rdv.iam.adapter.out.event;

import com.prdv.rdv.iam.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Abonne illustratif : notifie l'equipe de moderation lorsqu'un praticien
 * ou un etablissement soumet son dossier, et journalise les connexions
 * suspectes. D'autres abonnes (Slack, email, SIEM) se branchent pareillement.
 */
@Component
public class AdminNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationListener.class);

    @EventListener
    public void on(DomainEvent.PractitionerApplicationSubmitted event) {
        log.info("[MODERATION] Dossier praticien #{} (RPPS {}) en attente de validation manuelle",
                event.userId(), event.rpps());
    }

    @EventListener
    public void on(DomainEvent.EstablishmentApplicationSubmitted event) {
        log.info("[MODERATION] Etablissement '{}' (#{}) en attente de validation",
                event.legalName(), event.userId());
    }

    @EventListener
    public void on(DomainEvent.SuspiciousLoginDetected event) {
        log.warn("[SECURITE] Connexion suspecte pour #{} : {} depuis {}",
                event.userId(), event.reason(), event.ipAddress());
    }

    @EventListener
    public void on(DomainEvent.PractitionerReviewed event) {
        log.info("[MODERATION] Dossier #{} {} (motif : {})",
                event.userId(), event.approved() ? "approuve" : "refuse", event.reason());
    }
}
