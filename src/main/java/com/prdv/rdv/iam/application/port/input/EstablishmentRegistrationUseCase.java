package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.result.Views;

import java.util.List;

/**
 * Inscription et gestion des comptes entreprises / etablissements,
 * ainsi que la validation des rattachements praticiens (et remplaçants).
 */
public interface EstablishmentRegistrationUseCase {

    Views.OtpSentView register(RegistrationCommands.RegisterEstablishment command);

    Views.EstablishmentView currentProfile();

    List<Views.MembershipView> memberships();

    Views.MembershipView reviewMembership(ProfileCommands.ReviewMembership command);
}
