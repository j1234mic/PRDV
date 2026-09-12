package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.result.Views;

/**
 * Import des donnees du patient depuis une autre plateforme de rendez-vous
 * (couche anti-corruption : CSV, JSON, adapteurs Doctolib/Maiia...).
 */
public interface DataImportUseCase {

    Views.ImportSummary importMyProfile(ProfileCommands.ImportExternalProfile command);
}
