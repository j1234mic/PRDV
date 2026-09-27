package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/** Cas d'usage : dossier professionnel du praticien (module 2.2). */
public interface PractitionerProfileUseCase {

    ProfileViews.PractitionerDossierView myDossier();

    /** Dossier complet d'un praticien (utilise par l'annuaire et par les praticiens eux-memes). */
    ProfileViews.PractitionerDossierView dossierOf(Long practitionerUserId);

    ProfileViews.PractitionerDossierView updateIdentity(PractitionerProfileCommands.UpdateIdentity command);

    ProfileViews.PractitionerDossierView updatePracticeInformation(
            PractitionerProfileCommands.UpdatePracticeInformation command);

    ProfileViews.PractitionerDossierView updateManagement(
            PractitionerProfileCommands.UpdateManagement command);

    ProfileViews.PractitionerDossierView updateVisibility(
            PractitionerProfileCommands.UpdateVisibility command);

    ProfileViews.PractitionerDossierView addNetworkContact(
            PractitionerProfileCommands.AddNetworkContact command);

    ProfileViews.PractitionerDossierView removeNetworkContact(String contactId);

    ProfileViews.PractitionerDossierView uploadPhoto(PractitionerProfileCommands.StoreMedia command);

    ProfileViews.PractitionerDossierView uploadPresentationVideo(PractitionerProfileCommands.StoreMedia command);

    /** Recalcule les badges (verifie / populaire / nouveau) a partir des regles du domaine. */
    ProfileViews.PractitionerDossierView refreshBadges();
}
