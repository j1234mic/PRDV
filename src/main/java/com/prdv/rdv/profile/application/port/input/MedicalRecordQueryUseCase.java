package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.result.ProfileViews;

import java.util.List;

/**
 * Cas d'usage : lecture du dossier medical personnel (segregation des
 * interfaces : la lecture n'exige pas les memes droits que l'ecriture).
 */
public interface MedicalRecordQueryUseCase {

    /** Dossier du patient connecte. */
    ProfileViews.MedicalRecordView myRecord();

    /**
     * Dossier d'un patient par un tiers (praticien, secretaire deleguee).
     * L'acces est refuse si la regle de visibilite ou le consentement
     * ne le permet pas.
     */
    ProfileViews.MedicalRecordView recordOf(Long patientUserId);

    /** Rappels de vaccination echus ou a venir (fenetre en jours). */
    List<ProfileViews.VaccinationReminderView> vaccinationReminders(Long patientUserId, int lookaheadDays);
}
