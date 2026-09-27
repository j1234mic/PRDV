package com.prdv.rdv.profile.domain.event;

import com.prdv.rdv.common.domain.DomainEvent;

import java.time.Instant;

/**
 * Evenements de domaine du module « profils &amp; gestion des donnees ».
 *
 * <p>Publies via {@code ProfileEventPublisher} (adapteur Spring) : un abonne
 * peut declencher un email, alimenter un entrepot analytics, invalider un
 * cache ou notifier un praticien, sans que les services applicatifs aient a
 * etre modifies (Observer + Open/Closed).
 */
public interface ProfileEvent extends DomainEvent {

    /** Etat civil / coordonnees du patient mis a jour. */
    record PatientIdentityUpdated(Long userId, Instant occurredAt) implements ProfileEvent {
    }

    /** Une section du dossier medical personnel a change (antecedents, allergies...). */
    record MedicalRecordUpdated(Long userId, String section, Instant occurredAt) implements ProfileEvent {
    }

    /** Document medical televerse (une nouvelle version cree un evenement distinct). */
    record MedicalDocumentUploaded(Long userId, Long documentId, String category, int version,
                                   Instant occurredAt) implements ProfileEvent {
    }

    /** Document classe automatiquement (IA / regles) : la confiance permet un controle humain. */
    record MedicalDocumentClassified(Long documentId, String category, double confidence,
                                     String classifier, Instant occurredAt) implements ProfileEvent {
    }

    /** Document partage avec un praticien (tracabilite RGPD / secret medical). */
    record MedicalDocumentShared(Long userId, Long documentId, Long granteeUserId,
                                 String permission, Instant occurredAt) implements ProfileEvent {
    }

    /** Donnees de sante connectee synchronisees depuis un objet connecte. */
    record ConnectedDeviceSynchronized(Long userId, Long deviceId, String deviceType,
                                       int metricsImported, Instant occurredAt) implements ProfileEvent {
    }

    /** Seuil physiologique depasse : une alerte a ete declenchee. */
    record HealthAlertTriggered(Long userId, String metricType, String value, String severity,
                                String message, Instant occurredAt) implements ProfileEvent {
    }

    /** Consentement RGPD accorde ou retire. */
    record ConsentRecorded(Long userId, String purpose, boolean granted, Instant occurredAt)
            implements ProfileEvent {
    }

    /** Export de portabilite genere (RGPD art. 20). */
    record PatientDataExported(Long userId, String format, Instant occurredAt) implements ProfileEvent {
    }

    /** Demande d'effacement des donnees de profil (RGPD art. 17). */
    record ProfileErasureRequested(Long userId, Instant occurredAt) implements ProfileEvent {
    }

    /** Dossier professionnel d'un praticien mis a jour. */
    record PractitionerProfileUpdated(Long userId, String section, Instant occurredAt)
            implements ProfileEvent {
    }

    /** Badge de visibilite accorde (verifie, populaire, nouveau). */
    record PractitionerBadgeGranted(Long userId, String badge, String reason, Instant occurredAt)
            implements ProfileEvent {
    }

    /** Avis patient depose sur un praticien. */
    record PractitionerRated(Long practitionerUserId, Long patientUserId, int score,
                             Instant occurredAt) implements ProfileEvent {
    }
}
