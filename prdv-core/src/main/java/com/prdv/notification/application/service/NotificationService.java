package com.prdv.notification.application.service;

import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.model.User;
import com.prdv.notification.application.port.out.NotificationGateway;
import com.prdv.notification.domain.model.Notification;
import com.prdv.notification.domain.model.NotificationChannel;

/**
 * FACADE de notification : met en forme les messages standard, applique la
 * STRATEGIE de selection de canal, et delegue au gateway. Centraliser ici evite
 * la duplication dans chaque handler (DRY) et garde les handlers metier maigres (SRP).
 */
public final class NotificationService {

    private final NotificationGateway gateway;
    private final UserRepository users;

    public NotificationService(NotificationGateway gateway, UserRepository users) {
        this.gateway = gateway;
        this.users = users;
    }

    public void notifyAppointmentBooked(Long patientUserId, Long doctorUserId, String slotLabel,
                                        boolean autoConfirmed) {
        send(patientUserId, autoConfirmed ? "Rendez-vous enregistre" : "Demande de rendez-vous envoyee",
                (autoConfirmed ? "Votre rendez-vous du " : "Votre demande pour le ") + slotLabel
                        + (autoConfirmed ? " est enregistre." : " est transmise au praticien.")
                        + " Voir le detail dans l'application.",
                false);
        send(doctorUserId, "Nouveau rendez-vous",
                "Un patient a reserve le creneau du " + slotLabel + ".", true);
    }

    public void notifyAppointmentCancelled(Long patientUserId, Long doctorUserId, String slotLabel,
                                            String cancelledBy, int feeCents) {
        if ("PATIENT".equals(cancelledBy)) {
            String suffix = feeCents > 0
                    ? " Une penalite de " + (feeCents / 100) + " EUR est facturee (annulation tardive)."
                    : " Annulation sans frais, creneau remis en vente.";
            send(doctorUserId, "Annulation patient",
                    "Le creneau du " + slotLabel + " a ete annule." + suffix, true);
        } else {
            send(patientUserId, "Rendez-vous annule par le praticien",
                    "Votre rendez-vous du " + slotLabel + " a ete annule. Un nouveau creneau vous est "
                            + "propose automatiquement si vous etiez sur liste d'attente.",
                    true);
        }
    }

    public void notifyRescheduled(Long patientUserId, Long doctorUserId, String oldLabel, String newLabel) {
        send(patientUserId, "Rendez-vous deplace",
                "Votre rendez-vous du " + oldLabel + " est deplace au " + newLabel + ".", true);
        send(doctorUserId, "Deplacement de creneau",
                "Un patient a deplace son creneau au " + newLabel + ".", false);
    }

    public void notifyConfirmed(Long patientUserId, String slotLabel) {
        send(patientUserId, "Rendez-vous confirme",
                "Votre rendez-vous du " + slotLabel + " est confirme par le praticien.", false);
    }

    public void notifyWaitlistMatched(Long patientUserId, String slotLabel) {
        send(patientUserId, "Creneau libere : vous etes prioritaire",
                "Un creneau du " + slotLabel + " vient de se liberer et vous a ete attribue "
                        + "(liste d'attente). Annulez-le depuis l'application si vous n'etes pas disponible.",
                true);
    }

    public void notifyDoctorProfileDecision(Long doctorUserId, boolean approved, String reason) {
        send(doctorUserId,
                approved ? "Compte praticien verifie" : "Profil praticien refuse",
                approved ? "Votre profil est verifie : vous pouvez publier votre agenda."
                        : "Votre profil est refuse : " + reason + ". Corrigez et re-soumettez.",
                false);
    }

    public void send(Long recipientUserId, String subject, String body, boolean timeCritical) {
        User user = users.findById(recipientUserId).orElse(null);
        if (user == null) {
            return;
        }
        gateway.send(new Notification(recipientUserId, channelFor(user, timeCritical), subject, body));
    }

    /**
     * STRATEGIE "choix du canal par type" (module 5.1) : l'urgent part par SMS quand
     * le numero existe, le reste par email. Extrapolable vers un vrai service de reglage
     * par utilisateur quand la prefs table existera.
     */
    static NotificationChannel channelFor(User user, boolean timeCritical) {
        boolean hasPhone = user.phone() != null && !user.phone().isBlank();
        return (timeCritical && hasPhone) ? NotificationChannel.SMS : NotificationChannel.EMAIL;
    }
}
