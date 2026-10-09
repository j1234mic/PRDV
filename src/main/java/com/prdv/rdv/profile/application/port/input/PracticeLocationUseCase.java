package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

import java.util.List;

/** Cas d'usage : lieux d'exercice d'un praticien (multi-cabinets). */
public interface PracticeLocationUseCase {

    List<ProfileViews.PracticeLocationView> myLocations();

    List<ProfileViews.PracticeLocationView> locationsOf(Long practitionerUserId);

    ProfileViews.PracticeLocationView create(PractitionerProfileCommands.CreateLocation command);

    ProfileViews.PracticeLocationView update(String locationId,
                                             PractitionerProfileCommands.UpdateLocation command);

    void delete(String locationId);

    ProfileViews.PracticeLocationView addPhoto(PractitionerProfileCommands.AddLocationPhoto command);

    /** Supprime une photo de cabinet par son identifiant (photoId). */
    void removePhoto(String locationId, String photoId);

    /** Designe ce lieu comme principal ; l'ancien lieu principal n'est plus principal. */
    ProfileViews.PracticeLocationView promoteToMain(String locationId);
}
