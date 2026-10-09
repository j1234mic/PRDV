package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PractitionerProfileCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.practitioner.Badge;

import java.util.List;

/** Cas d'usage : annuaire public des praticiens, avis et badges. */
public interface PractitionerDirectoryUseCase {

    /** Fiche publique d'un praticien (filtrée selon les regles de visibilite). */
    ProfileViews.PractitionerDossierView publicProfile(Long practitionerUserId);

    /**
     * Recherche publique. Le filtre {@code badge} (VERIFIED, POPULAR ou NEW) ne
     * retient que les praticiens qui portent ce badge a l'instant de la requete.
     */
    ProfileViews.PagedResult<ProfileViews.DirectoryEntryView> search(String term,
                                                                    String specialty,
                                                                    String city,
                                                                    boolean teleconsultationOnly,
                                                                    boolean wheelchairAccessibleOnly,
                                                                    Badge.BadgeType badge,
                                                                    int page,
                                                                    int size);

    List<ProfileViews.PractitionerRatingView> ratingsOf(Long practitionerUserId);

    /** Un patient ne peut noter un praticien qu'une seule fois. */
    ProfileViews.PractitionerRatingView submitRating(Long practitionerUserId,
                                                     PractitionerProfileCommands.SubmitRating command);

    /** Photo professionnelle d'une fiche publiable. */
    ProfileViews.DocumentFile practitionerPhoto(Long practitionerUserId);

    /** Video de presentation d'une fiche publiable. */
    ProfileViews.DocumentFile presentationVideo(Long practitionerUserId);

    /** Photo d'un cabinet appartenant a une fiche publiable. */
    ProfileViews.DocumentFile locationPhoto(Long practitionerUserId, String locationId, String photoId);
}
