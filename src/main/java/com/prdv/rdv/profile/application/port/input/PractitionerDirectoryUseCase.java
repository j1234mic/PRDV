package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

import java.util.List;

/** Cas d'usage : annuaire public des praticiens, avis et badges. */
public interface PractitionerDirectoryUseCase {

    /** Fiche publique d'un praticien (filtrée selon les regles de visibilite). */
    ProfileViews.PractitionerDossierView publicProfile(Long practitionerUserId);

    ProfileViews.PagedResult<ProfileViews.DirectoryEntryView> search(String term,
                                                                    String specialty,
                                                                    String city,
                                                                    boolean teleconsultationOnly,
                                                                    boolean wheelchairAccessibleOnly,
                                                                    int page,
                                                                    int size);

    List<ProfileViews.PractitionerRatingView> ratingsOf(Long practitionerUserId);

    /** Un patient ne peut noter un praticien qu'une seule fois. */
    ProfileViews.PractitionerRatingView submitRating(Long practitionerUserId,
                                                     PractitionerProfileCommands.SubmitRating command);
}
