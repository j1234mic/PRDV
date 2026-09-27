package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.PrivacyCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/**
 * Cas d'usage : portabilite (RGPD art. 20) et effacement (RGPD art. 17)
 * des donnees de profil.
 */
public interface DataPortabilityUseCase {

    /** Export complet et lisible par machine du profil, du dossier medical et des consentements. */
    ProfileViews.DataExportView exportMyData(String format);

    /** Effacement des donnees du module profils puis demande d'anonymisation du compte. */
    void requestErasure(PrivacyCommands.RequestErasure command);
}
